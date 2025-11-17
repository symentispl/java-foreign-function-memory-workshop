package pl.symentis.workshops.foreign.metrics;

import org.apache.fory.serializer.StringSerializer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;

/**
 * Off-heap metrics storage using Java Foreign Memory API.
 *
 * <p>Stores metrics data (timestamp, value, name) in native memory with variable-length
 * string names. Uses StructLayout for fixed fields and natural alignment.
 *
 * <p>Memory layout for each entry:
 * <pre>
 * [timestamp:8][value:8][nameLength:4][nameBytes:variable]
 * </pre>
 *
 * <p>The double value is naturally aligned at offset 8, eliminating the need for
 * padding calculations. All fixed-size fields are managed by a single StructLayout.
 *
 * <p>Usage example:
 * <pre>
 * try (var arena = Arena.ofConfined()) {
 *     var storage = new OffHeapMetricsStorage(arena, 4096);
 *     var builder = storage.builder();
 *
 *     // Write metrics
 *     builder.addMetric(System.currentTimeMillis(), "cpu.usage", 75.5);
 *     builder.addMetric(System.currentTimeMillis(), "memory.free", 1024.0);
 *
 *     // Read metrics
 *     var cursor = storage.cursor();
 *     while (cursor.hasNext()) {
 *         System.out.printf("%d: %s = %.2f%n",
 *             cursor.getTimestamp(),
 *             cursor.getMetricName(),
 *             cursor.getValue());
 *         cursor.next();
 *     }
 * }
 * </pre>
 */
public class OffHeapMetricsStorage {

    /**
     * Maximum allowed metric name length in bytes (UTF-8)
     */
    public static final int MAX_NAME_LENGTH = 255;

    /**
     * Fixed header layout: timestamp (8) + value (8) + nameLength (4) + padding (4) = 24 bytes.
     * Padding ensures the entire struct is 8-byte aligned for optimal memory access.
     */
    private static final MemoryLayout HEADER_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("timestamp"),
            ValueLayout.JAVA_DOUBLE.withName("value"),
            ValueLayout.JAVA_INT.withName("nameLength"),
            MemoryLayout.paddingLayout(4)
    );

    private static final VarHandle TIMESTAMP_VAR = HEADER_LAYOUT.varHandle(groupElement("timestamp"));
    private static final VarHandle VALUE_VAR = HEADER_LAYOUT.varHandle(groupElement("value"));
    private static final VarHandle NAME_LENGTH_VAR = HEADER_LAYOUT.varHandle(groupElement("nameLength"));


    /**
     * Size of fixed header in bytes
     */
    private static final long HEADER_SIZE = HEADER_LAYOUT.byteSize(); // 24 bytes

    /**
     * Minimum entry size: header + empty name
     */
    private static final long MIN_ENTRY_SIZE = HEADER_SIZE;

    private final MemorySegment segment;
    private long bytesUsed;
    private int entryCount;

    /**
     * Creates a new off-heap metrics storage.
     *
     * @param arena         the memory arena for allocation
     * @param capacityBytes the total capacity in bytes
     * @throws IllegalArgumentException if capacity is too small
     */
    public OffHeapMetricsStorage(Arena arena, long capacityBytes) {
        if (capacityBytes < MIN_ENTRY_SIZE) { // if you never take branch
            throw new IllegalArgumentException(
                    "Capacity must be at least %d bytes".formatted(MIN_ENTRY_SIZE));
        }
        // Allocate with 8-byte alignment to enable aligned LONG and DOUBLE access
        this.segment = arena.allocate(capacityBytes, 8);
        this.bytesUsed = 0;
        this.entryCount = 0;
    }

    /**
     * Returns the total capacity in bytes.
     */
    public long capacity() {
        return segment.byteSize();
    }

    /**
     * Returns the number of bytes currently used.
     */
    public long bytesUsed() {
        return bytesUsed;
    }

    /**
     * Returns the number of bytes remaining for new entries.
     */
    public long bytesRemaining() {
        return capacity() - bytesUsed;
    }

    /**
     * Returns the number of metric entries stored.
     */
    public int entryCount() {
        return entryCount;
    }

    /**
     * Creates a new builder for writing metrics.
     *
     * @return a new MetricsBuilder instance
     */
    public MetricsBuilder builder() {
        return new MetricsBuilder();
    }

    /**
     * Creates a new cursor for reading metrics.
     *
     * @return a new MetricsCursor instance
     */
    public MetricsCursor cursor() {
        return new MetricsCursor();
    }

    /**
     * Calculates the size for a metric entry.
     * Entry size is rounded up to 8-byte boundary to ensure next entry's struct is aligned.
     *
     * @param nameLength the length of the metric name in bytes
     * @return the total size aligned to 8 bytes
     */
    private static long calculateEntrySize(int nameLength) {
        long size = HEADER_SIZE + nameLength;
        // Round up to next 8-byte boundary for next entry's alignment
        return (size + 7) & ~7;
    }

    /**
     * Builder for writing metrics to off-heap storage.
     * Provides a write-only interface with automatic position tracking.
     */
    public class MetricsBuilder {


        private MetricsBuilder() {
            // Package-private constructor
        }

        /**
         * Adds a metric entry to the storage.
         *
         * @param timestamp  the timestamp in milliseconds
         * @param metricName the metric name (max 255 bytes UTF-8)
         * @param value      the metric value
         * @throws IllegalArgumentException if name is too long
         * @throws IllegalStateException    if insufficient space remains
         */
        public void addMetric(long timestamp, String metricName, double value) {
            if (metricName == null) {
                throw new IllegalArgumentException("Metric name cannot be null");
            }
            var metricNameAsBytes = metricName.getBytes(StandardCharsets.UTF_8);

            if (metricNameAsBytes.length > MAX_NAME_LENGTH) {
                throw new IllegalArgumentException(
                        "Metric name too long: %d bytes (max %d)".formatted(metricNameAsBytes.length, MAX_NAME_LENGTH));
            }

            // Align current position to 8-byte boundary for optimal memory access
            var alignedOffset = (bytesUsed + 7) & ~7;
            var entrySize = calculateEntrySize(metricNameAsBytes.length);

            if (alignedOffset + entrySize > segment.byteSize()) {
                throw new IllegalStateException(
                        "Insufficient space: need %d bytes, but only %d available".formatted(entrySize, bytesRemaining()));
            }

            // Write fixed header fields using aligned memory access
            TIMESTAMP_VAR.set(segment, alignedOffset, timestamp);
            VALUE_VAR.set(segment, alignedOffset, value);
            NAME_LENGTH_VAR.set(segment, alignedOffset, metricNameAsBytes.length);

            // Write name bytes immediately after fixed header
            var nameOffset = alignedOffset + HEADER_SIZE;
            MemorySegment.copy(metricNameAsBytes, 0, segment, ValueLayout.JAVA_BYTE, nameOffset, metricNameAsBytes.length);

            // Update tracking - use aligned offset + entry size
            bytesUsed = alignedOffset + entrySize;
            entryCount++;
        }

        /**
         * Returns the number of entries written.
         */
        public int count() {
            return entryCount;
        }
    }

    /**
     * Cursor for reading metrics from off-heap storage.
     * Provides iterator-like navigation with typed field access.
     */
    public class MetricsCursor {

        private long currentOffset;

        MetricsCursor() {
            this.currentOffset = 0;
        }

        /**
         * Checks if there are more entries to read.
         *
         * @return true if more entries exist
         */
        public boolean hasNext() {
            return currentOffset < bytesUsed &&
                   currentOffset + MIN_ENTRY_SIZE <= segment.byteSize();
        }

        /**
         * Advances to the next entry.
         *
         * @throws NoSuchElementException if no more entries exist
         */
        public void next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more entries");
            }

            int nameLength = getNameLength();
            currentOffset += calculateEntrySize(nameLength);
        }

        /**
         * Resets the cursor to the beginning.
         */
        public void reset() {
            currentOffset = 0;
        }

        /**
         * Returns the current position in bytes.
         */
        public long position() {
            return currentOffset;
        }

        /**
         * Seeks to a specific byte offset.
         *
         * @param offset the target offset
         * @throws IllegalArgumentException if offset is invalid
         */
        public void seek(long offset) {
            if (offset < 0 || offset >= bytesUsed) {
                throw new IllegalArgumentException(
                        "Invalid offset: %d (valid range: 0-%d)".formatted(offset, bytesUsed));
            }
            currentOffset = offset;
        }

        /**
         * Gets the timestamp of the current entry.
         *
         * @return the timestamp in milliseconds
         */
        public long getTimestamp() {
            checkBounds();
            return (long) TIMESTAMP_VAR.get(segment, currentOffset);
        }

        /**
         * Gets the name length of the current entry.
         *
         * @return the name length in bytes
         */
        public int getNameLength() {
            checkBounds();
            return (int) NAME_LENGTH_VAR.get(segment, currentOffset);
        }

        /**
         * Gets the metric name of the current entry.
         *
         * @return the metric name as a String
         */
        public String getMetricName() {
            checkBounds();
            int nameLength = getNameLength();
            long nameOffset = currentOffset + HEADER_SIZE;

            byte[] nameBytes = new byte[nameLength];

            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, nameOffset, nameBytes, 0, nameLength);
            return StringSerializer.newBytesStringZeroCopy((byte) 0, nameBytes);
        }

        /**
         * Gets the value of the current entry.
         *
         * @return the metric value
         */
        public double getValue() {
            checkBounds();
            return (double) VALUE_VAR.get(segment, currentOffset);
        }

        private void checkBounds() {
            if (currentOffset >= bytesUsed) {
                throw new IllegalStateException(
                        "Cursor is past the last entry (position: %d, bytesUsed: %d)".formatted(currentOffset, bytesUsed));
            }
        }
    }
}
