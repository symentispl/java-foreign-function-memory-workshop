package pl.symentis.workshops.foreign.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

import static java.lang.System.out;

/**
 * The Example6 class demonstrates the use of the Foreign Memory API for working with memory segments
 * and sequence-based memory layouts in Java. It showcases how to allocate memory for a sequence layout,
 * manipulate values within the sequence using a VarHandle, and retrieve metadata about the method handle types.
 *
 * Key features demonstrated in this class:
 * - Allocating a sequence layout consisting of 16 elements, each corresponding to the size of a Java int.
 * - Using VarHandle to access and modify specific elements of the sequence layout.
 * - Printing and understanding method handle types generated from VarHandle for different access modes.
 *
 * This class is an example of confined memory usage, ensuring the allocated memory is managed
 * within the scope of the arena and released properly upon completion.
 */
public class Example6 {

    static void main() {
        MemorySegment memorySegment;
        try (var arena = Arena.ofConfined()) {
            // create array of 16 elements of Java int size
            var sequenceLayout = MemoryLayout.sequenceLayout(16, ValueLayout.JAVA_INT);
            memorySegment = arena.allocate(sequenceLayout);
            var varHandle = sequenceLayout.varHandle(PathElement.sequenceElement(1));
            varHandle.set(memorySegment, 0, 666);
            var value = varHandle.get(memorySegment, 0);
            out.printf("value at index zero is %d%n", value);
            out.printf(
                    "method handle type for get access mode is %s\n",
                    varHandle.toMethodHandle(VarHandle.AccessMode.GET).type());
            out.printf(
                    "method handle type for set access mode is %s\n",
                    varHandle.toMethodHandle(VarHandle.AccessMode.SET).type());
        }
    }
}
