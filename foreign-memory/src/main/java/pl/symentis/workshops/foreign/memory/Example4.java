package pl.symentis.workshops.foreign.memory;

import java.lang.foreign.Arena;

/**
 * The Example4 class demonstrates the use of the Foreign Memory API with an automatic memory scope.
 *
 * Key points about the class:
 * - Utilizes the `Arena.ofAuto()` method to allocate memory with an automatic scope.
 * - Memory allocated in this scope is automatically managed and cleared when the garbage collector
 *   removes the associated memory segment.
 *
 * This example focuses on showcasing the use of automatic memory arenas to efficiently manage memory
 * without requiring manual management or explicit resource closure.
 */
public class Example4 {

    static void main() {
        // since this is auto scope, memory segment will be cleared once GC removes segment
        var segment = Arena.ofAuto().allocate(1024);
    }
}
