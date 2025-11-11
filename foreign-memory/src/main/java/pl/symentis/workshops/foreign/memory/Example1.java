package pl.symentis.workshops.foreign.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

import static java.lang.System.out;

public class Example1 {

    static void main() {
        try (var arena = Arena.ofConfined()) {
            var memorySegment = arena.allocate(1024);
            memorySegment.setAtIndex(ValueLayout.JAVA_LONG, 1, 666);
            out.printf("value at index zero is %d%n", memorySegment.getAtIndex(ValueLayout.JAVA_LONG, 1));
            out.printf("value at offset zero is %d%n", memorySegment.get(ValueLayout.JAVA_LONG, 1));
        }
    }
}
