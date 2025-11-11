package pl.symentis.workshops.foreign.posix;

import pl.symentis.foreign.posix.errno.errno_h;
import pl.symentis.foreign.posix.mqueue.mqueue_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Scanner;

import static java.lang.System.out;

/**
 * A class that demonstrates sending messages to a POSIX message queue.
 *
 * <p>This class includes functionality to interact with a POSIX message queue
 * for sending string messages. The implementation makes use of the Foreign
 * Function & Memory API (FFM API) for allocating off-heap memory and native
 * invocations related to interacting with POSIX message queues.
 *
 * <p>Messages can be sent to the queue by typing them into the standard input,
 * and the application continues until the user explicitly interrupts (e.g., using
 * Ctrl+D or Ctrl+C).
 * <p>
 * This class is designed for demonstration and educational purposes and should not
 * be considered production-grade.
 * <p>
 * Usage notes:
 * - The program interacts with a named POSIX message queue. Ensure that the message
 * queue interface is supported on the system where it is run.
 * - Permissions and attributes are set during queue creation. If the queue already
 * exists, it is reused.
 * - Messages sent are simple strings and are transmitted with a priority of 0.
 * - Proper cleanup of resources is performed by closing the queue after use.
 * - The sender allocates memory off-heap for communicating with the POSIX message
 * queue, and memory is managed within the scope of an Arena.
 * <p>
 * Key methods:
 * - The {@code main()} method contains the full logic for interacting with the
 * POSIX message queue, including opening the queue, sending messages, and
 * closing the queue upon exit.
 * - The {@code errno()} method retrieves the current value of the global {@code errno}
 * for error handling in native function calls.
 * <p>
 * Prerequisites:
 * - The necessary native libraries and definitions for working with message queues
 * must be available. Specifically, native bindings for POSIX message queue
 * functions such as {@code mq_open}, {@code mq_send}, and {@code mq_close} are used.
 */
public class PosixQueueSender {

    static void main() {
        try (var arena = Arena.ofConfined()) {
            var queueName = "/queue";
            var posixQueueName = arena.allocateFrom(queueName);

            // Open the queue in write mode
            // Using the same queue name as the receiver
            var queue_desc = mqueue_h.mq_open.makeInvoker(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
                    .apply(posixQueueName,
                            mqueue_h.O_WRONLY() | mqueue_h.O_CREAT(),
                            0664, // permission of queue
                            MemorySegment.NULL); // use default attributes

            if (queue_desc == -1) {
                int errno = errno();
                throw new RuntimeException("failed to mq_open with errno %d".formatted(errno));
            }

            out.printf("Opened POSIX queue %s for sending messages%n", queueName);
            out.println("Type messages and press Enter to send (Ctrl+D or Ctrl+C to exit):");

            // Read from stdin and send messages
            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    String message = scanner.nextLine();

                    if (message.isEmpty()) {
                        continue;
                    }

                    // Allocate memory for the message
                    var messageSegment = arena.allocateFrom(message);

                    // Send the message with priority 0
                    var result = mqueue_h.mq_send(queue_desc, messageSegment, message.length() + 1, 0);

                    if (result == -1) {
                        int errno = errno();
                        throw new RuntimeException("failed to mq_send with errno %d".formatted(errno));
                    }

                    out.printf("Message sent: %s%n", message);
                }
            }

            // Close the queue
            if (mqueue_h.mq_close(queue_desc) == -1) {
                int errno = errno();
                out.printf("Warning: failed to mq_close with errno %d%n", errno);
            }

            out.println("Sender shutting down...");
        }
    }

    private static int errno() {
        return errno_h.__errno_location().get(ValueLayout.JAVA_INT, 0);
    }
}