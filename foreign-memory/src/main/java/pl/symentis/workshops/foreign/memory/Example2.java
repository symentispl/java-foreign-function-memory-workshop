package pl.symentis.workshops.foreign.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * The Example2 class demonstrates the incorrect handling of a memory segment allocated
 * within a confined memory arena.
 *
 * Key points regarding this implementation:
 * - Memory is allocated using `Arena.ofConfined()` which enforces confined scope memory management.
 * - Attempts to use the memory segment outside the try-with-resources block where the arena has been closed, leading to undefined behavior.
 * - The method improperly accesses or modifies the memory segment after the arena is closed, which is not allowed.
 *
 * This example highlights the critical importance of managing memory scope and lifecycle
 * correctly when using the Foreign Memory API.
 */
public class Example2 {

    static void main() {
        MemorySegment memorySegment;
        try (var arena = Arena.ofConfined()) {
            memorySegment = arena.allocate(1024);
        }
        memorySegment.setAtIndex(ValueLayout.JAVA_LONG, 1, 666);
    }
}
