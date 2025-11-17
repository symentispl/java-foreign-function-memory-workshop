package pl.symentis.workshops.foreign.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

import static java.lang.System.out;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;

/**
 * The Example7 class demonstrates advanced usage of the Foreign Memory API
 * introduced in newer versions of Java. It showcases working with memory layouts,
 * structured memory segments, variable handles, and method handles for memory operations.
 *
 * Key concepts covered in this class include:
 * - Defining structured memory layouts with fields of various types and nested structures.
 * - Allocating and managing memory segments within confined memory arenas.
 * - Using VarHandle to access and manipulate specific fields within a structured memory segment.
 * - Converting a VarHandle to a MethodHandle for advanced operations, including binding memory segments
 *   or using volatile access modes.
 */
public class Example7 {

    static void main() throws Throwable {
        try (var arena = Arena.ofAuto()) {

            var structLayout = MemoryLayout.structLayout(
                    ValueLayout.JAVA_LONG.withName("year"),
                    ValueLayout.JAVA_SHORT.withName("month"),
                    ValueLayout.JAVA_SHORT.withName("day"),
                    MemoryLayout.structLayout(ValueLayout.JAVA_SHORT.withName("offset"))
                            .withName("timezone"));

            var memorySegment = arena.allocate(structLayout);
            var yearVarHandler = structLayout.varHandle(groupElement("year"));
            out.println("year struct field var handler " + yearVarHandler);
            yearVarHandler.set(memorySegment, 0, 1975L);
            var year = yearVarHandler.get(memorySegment, 0);
            out.printf("year is %d%n", year);


            var timezoneOffsetVarHandler = structLayout.varHandle(groupElement("timezone"), groupElement("offset"));
            timezoneOffsetVarHandler.set(memorySegment, 0, (short) 1);
            var timezoneOffset = timezoneOffsetVarHandler.get(memorySegment, 0);
            out.printf("timezone offset is %d%n", timezoneOffset);

            // here comes method handle magic, this time parameter binding
            var timezoneOffsetmethodHandle = timezoneOffsetVarHandler
                    .toMethodHandle(VarHandle.AccessMode.GET)
                    .bindTo(memorySegment);
            out.println("method handle " + timezoneOffsetmethodHandle.type());
            out.printf("timezone offset using method handler is %d%n", timezoneOffsetmethodHandle.invoke(0));

            // even more magic with method handle, this time volatile access mode
            timezoneOffsetmethodHandle = timezoneOffsetVarHandler.toMethodHandle(VarHandle.AccessMode.SET_RELEASE);
            timezoneOffsetmethodHandle = timezoneOffsetVarHandler.toMethodHandle(VarHandle.AccessMode.SET_RELEASE);
            out.printf(
                    "timezone offset using method handler is volatile access is %d%n",
                    timezoneOffsetmethodHandle.invoke(memorySegment, 0));
        }
    }
}
