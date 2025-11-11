package pl.symentis.workshops.foreign.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

import static java.lang.System.out;

/**
 * The Example0 class demonstrates the use of the Foreign Memory API to allocate memory and
 * interact with memory segments using a global memory arena.
 *
 * Key points about this class:
 * - Allocates a memory segment using a global memory arena (`Arena.global()`).
 * - Writes a value to the memory segment at a specified offset using `memorySegment.set()`.
 * - Reads and displays the value from the memory segment at the same offset using `memorySegment.get()`.
 *
 * This example highlights the use of a global arena for memory management, avoiding explicit
 * resource closure, and how to manipulate memory segments at specific offsets.
 */
public class Example0 {
    static void main() {
        var arena = Arena.global();
        var memorySegment = arena.allocate(1024);
        memorySegment.set(ValueLayout.JAVA_LONG, 0, 666);
        out.printf("value at offset zero is %d\n", memorySegment.get(ValueLayout.JAVA_LONG, 0));
    }
}
