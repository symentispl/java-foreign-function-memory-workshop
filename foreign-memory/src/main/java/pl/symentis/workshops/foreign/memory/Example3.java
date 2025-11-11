package pl.symentis.workshops.foreign.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * The Example3 class demonstrates the usage of the Foreign Memory API in a multi-threaded environment.
 *
 * This example explores the following aspects:
 * - Allocation of a shared memory arena using `Arena.ofShared()`.
 * - Modifying a memory segment concurrently in a separate thread.
 * - Managing thread synchronization using the `Thread` API, including `start()` and `join()`.
 * - Correct handling of exceptions, particularly `InterruptedException`.
 *
 * The class showcases the flexibility of shared memory arenas in facilitating concurrent access and
 * modification but also emphasizes the importance of managing memory lifecycle and thread operations carefully.
 */
public class Example3 {

    static void main() {
        MemorySegment memorySegment;
        try (var arena = Arena.ofShared()) {
            memorySegment = arena.allocate(1024);
            var thread = new Thread(() -> {
                memorySegment.setAtIndex(ValueLayout.JAVA_LONG, 1, 666);
            });
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
