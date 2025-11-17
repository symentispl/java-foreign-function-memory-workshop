package pl.symentis.workshops.foreign.metrics;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

import static org.assertj.core.api.Assertions.*;

/**
 * JUnit test for OffHeapMetricsStorage demonstrating off-heap metrics storage
 * with variable-length entries.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>StructLayout for managing fixed-size fields</li>
 *   <li>VarHandles derived from StructLayout for automatic offset management</li>
 *   <li>Variable-length struct layout using length-prefix pattern</li>
 *   <li>Natural memory alignment (double at offset 8 - no padding needed!)</li>
 *   <li>Builder pattern for writing metrics</li>
 *   <li>Cursor pattern for iterator-like reading</li>
 *   <li>Database-like scanning over off-heap memory</li>
 * </ul>
 *
 * <p>Converted from Example8.java main() demonstration.
 */
class OffHeapMetricsStorageTest {

    @Test
    void testBasicWriteAndRead() {
        // Create storage with 2KB capacity using confined arena
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);

            assertThat(storage.capacity()).isEqualTo(2048);
            assertThat(storage.bytesUsed()).isZero();
            assertThat(storage.entryCount()).isZero();

            // Write metrics
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            builder.addMetric(timestamp, "cpu.usage", 75.5);
            builder.addMetric(timestamp + 1000, "memory.free", 2048.0);
            builder.addMetric(timestamp + 2000, "disk.io.read", 1024.5);
            builder.addMetric(timestamp + 3000, "disk.io.write", 512.25);
            builder.addMetric(timestamp + 4000, "network.rx", 8192.0);
            builder.addMetric(timestamp + 5000, "network.tx", 4096.0);

            assertThat(builder.count()).isEqualTo(6);
            assertThat(storage.entryCount()).isEqualTo(6);
            assertThat(storage.bytesUsed()).isPositive();
            assertThat(storage.bytesUsed()).isLessThan(storage.capacity());

            // Read metrics and verify
            var cursor = storage.cursor();

            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.getTimestamp()).isEqualTo(timestamp);
            assertThat(cursor.getMetricName()).isEqualTo("cpu.usage");
            assertThat(cursor.getValue()).isCloseTo(75.5, within(0.001));
            cursor.next();

            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.getTimestamp()).isEqualTo(timestamp + 1000);
            assertThat(cursor.getMetricName()).isEqualTo("memory.free");
            assertThat(cursor.getValue()).isCloseTo(2048.0, within(0.001));
            cursor.next();

            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.getTimestamp()).isEqualTo(timestamp + 2000);
            assertThat(cursor.getMetricName()).isEqualTo("disk.io.read");
            assertThat(cursor.getValue()).isCloseTo(1024.5, within(0.001));
            cursor.next();

            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.getTimestamp()).isEqualTo(timestamp + 3000);
            assertThat(cursor.getMetricName()).isEqualTo("disk.io.write");
            assertThat(cursor.getValue()).isCloseTo(512.25, within(0.001));
            cursor.next();

            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.getTimestamp()).isEqualTo(timestamp + 4000);
            assertThat(cursor.getMetricName()).isEqualTo("network.rx");
            assertThat(cursor.getValue()).isCloseTo(8192.0, within(0.001));
            cursor.next();

            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.getTimestamp()).isEqualTo(timestamp + 5000);
            assertThat(cursor.getMetricName()).isEqualTo("network.tx");
            assertThat(cursor.getValue()).isCloseTo(4096.0, within(0.001));
            cursor.next();

            assertThat(cursor.hasNext()).isFalse();
        }
    }

    @Test
    void testIteratorStyleReading() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            builder.addMetric(timestamp, "cpu.usage", 75.5);
            builder.addMetric(timestamp + 1000, "memory.free", 2048.0);
            builder.addMetric(timestamp + 2000, "disk.io.read", 1024.5);

            // Iterator-style reading
            var cursor = storage.cursor();
            int count = 0;

            while (cursor.hasNext()) {
                assertThat(cursor.getMetricName()).isNotNull();
                assertThat(cursor.getValue()).isGreaterThanOrEqualTo(0);
                assertThat(cursor.getTimestamp()).isGreaterThanOrEqualTo(timestamp);
                count++;
                cursor.next();
            }

            assertThat(count).isEqualTo(3);
        }
    }

    @Test
    void testCursorReset() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            builder.addMetric(timestamp, "cpu.usage", 75.5);
            builder.addMetric(timestamp + 1000, "memory.free", 2048.0);
            builder.addMetric(timestamp + 2000, "disk.io.read", 1024.5);

            // First read
            var cursor = storage.cursor();
            int count1 = 0;
            double sum1 = 0.0;

            while (cursor.hasNext()) {
                sum1 += cursor.getValue();
                count1++;
                cursor.next();
            }

            assertThat(count1).isEqualTo(3);
            assertThat(cursor.hasNext()).isFalse();

            // Reset and re-read
            cursor.reset();
            int count2 = 0;
            double sum2 = 0.0;

            while (cursor.hasNext()) {
                sum2 += cursor.getValue();
                count2++;
                cursor.next();
            }

            assertThat(count2).isEqualTo(count1);
            assertThat(sum2).isCloseTo(sum1, within(0.001));
        }
    }

    @Test
    void testVariableLengthNames() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            // Add metrics with different name lengths
            builder.addMetric(timestamp, "cpu", 1.0);           // Short name (3 bytes)
            builder.addMetric(timestamp, "memory.free", 2.0);   // Medium name (11 bytes)
            builder.addMetric(timestamp, "disk.io.read.throughput.bytes", 3.0);  // Long name (29 bytes)

            var cursor = storage.cursor();

            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.getNameLength()).isEqualTo(3);
            assertThat(cursor.getMetricName()).isEqualTo("cpu");
            cursor.next();

            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.getNameLength()).isEqualTo(11);
            assertThat(cursor.getMetricName()).isEqualTo("memory.free");
            cursor.next();

            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.getNameLength()).isEqualTo(29);
            assertThat(cursor.getMetricName()).isEqualTo("disk.io.read.throughput.bytes");
            cursor.next();

            assertThat(cursor.hasNext()).isFalse();
        }
    }

    @Test
    void testPositionAndSeek() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            builder.addMetric(timestamp, "first", 1.0);
            builder.addMetric(timestamp + 1000, "second", 2.0);
            builder.addMetric(timestamp + 2000, "third", 3.0);

            var cursor = storage.cursor();

            // Navigate to second entry
            assertThat(cursor.hasNext()).isTrue();
            assertThat(cursor.position()).isZero();
            cursor.next();

            assertThat(cursor.hasNext()).isTrue();
            long secondEntryPosition = cursor.position();
            assertThat(secondEntryPosition).isPositive();
            assertThat(cursor.getMetricName()).isEqualTo("second");
            assertThat(cursor.getValue()).isCloseTo(2.0, within(0.001));

            // Navigate to third entry
            cursor.next();
            assertThat(cursor.getMetricName()).isEqualTo("third");

            // Seek back to second entry
            cursor.seek(secondEntryPosition);
            assertThat(cursor.position()).isEqualTo(secondEntryPosition);
            assertThat(cursor.getMetricName()).isEqualTo("second");
            assertThat(cursor.getValue()).isCloseTo(2.0, within(0.001));
        }
    }

    @Test
    void testAverageCalculation() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            builder.addMetric(timestamp, "cpu.usage", 75.5);
            builder.addMetric(timestamp + 1000, "memory.free", 2048.0);
            builder.addMetric(timestamp + 2000, "disk.io.read", 1024.5);
            builder.addMetric(timestamp + 3000, "disk.io.write", 512.25);
            builder.addMetric(timestamp + 4000, "network.rx", 8192.0);
            builder.addMetric(timestamp + 5000, "network.tx", 4096.0);

            var cursor = storage.cursor();
            int count = 0;
            double sum = 0.0;

            while (cursor.hasNext()) {
                sum += cursor.getValue();
                count++;
                cursor.next();
            }

            assertThat(count).isEqualTo(6);
            double expectedAverage = (75.5 + 2048.0 + 1024.5 + 512.25 + 8192.0 + 4096.0) / 6;
            assertThat(sum / count).isCloseTo(expectedAverage, within(0.001));
        }
    }

    @Test
    void testMemoryLayoutInformation() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            builder.addMetric(timestamp, "cpu.usage", 75.5);
            builder.addMetric(timestamp + 1000, "memory.free", 2048.0);

            assertThat(OffHeapMetricsStorage.MAX_NAME_LENGTH).isEqualTo(255);
            assertThat(storage.entryCount()).isEqualTo(2);
            assertThat(storage.bytesUsed()).isPositive();
            assertThat(storage.bytesRemaining()).isPositive();
            assertThat(storage.capacity()).isEqualTo(storage.bytesUsed() + storage.bytesRemaining());

            double averageEntrySize = (double) storage.bytesUsed() / storage.entryCount();
            assertThat(averageEntrySize).isPositive();
        }
    }

    @Test
    void testCapacityExceeded() {
        try (var arena = Arena.ofConfined()) {
            // Create small storage (100 bytes)
            var storage = new OffHeapMetricsStorage(arena, 100);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            // Add first metric - should succeed
            builder.addMetric(timestamp, "metric1", 1.0);

            // Try to add many metrics until capacity is exceeded
            assertThatThrownBy(() -> {
                for (int i = 2; i < 100; i++) {
                    builder.addMetric(timestamp + i * 1000, "metric" + i, i * 1.0);
                }
            }).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void testNameTooLong() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            // Create a name longer than MAX_NAME_LENGTH (255 bytes)
            String longName = "a".repeat(256);

            assertThatThrownBy(() -> {
                builder.addMetric(timestamp, longName, 1.0);
            }).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void testNullMetricName() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            assertThatThrownBy(() -> {
                builder.addMetric(timestamp, null, 1.0);
            }).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void testInvalidSeek() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            builder.addMetric(timestamp, "metric1", 1.0);

            var cursor = storage.cursor();

            // Seek to negative offset
            assertThatThrownBy(() -> {
                cursor.seek(-1);
            }).isInstanceOf(IllegalArgumentException.class);

            // Seek beyond used bytes
            assertThatThrownBy(() -> {
                cursor.seek(storage.bytesUsed());
            }).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void testCursorBeyondEnd() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var builder = storage.builder();
            long timestamp = System.currentTimeMillis();

            builder.addMetric(timestamp, "metric1", 1.0);

            var cursor = storage.cursor();

            // Navigate past the end
            cursor.next();
            assertThat(cursor.hasNext()).isFalse();

            // Try to navigate further
            assertThatThrownBy(cursor::next).isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    @Test
    void testEmptyStorage() {
        try (var arena = Arena.ofConfined()) {
            var storage = new OffHeapMetricsStorage(arena, 2048);
            var cursor = storage.cursor();

            assertThat(cursor.hasNext()).isFalse();
            assertThat(storage.entryCount()).isZero();
            assertThat(storage.bytesUsed()).isZero();
        }
    }

    @Test
    void testMinimumCapacity() {
        try (var arena = Arena.ofConfined()) {
            // Should fail with capacity too small
            assertThatThrownBy(() -> {
                new OffHeapMetricsStorage(arena, 10);
            }).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
