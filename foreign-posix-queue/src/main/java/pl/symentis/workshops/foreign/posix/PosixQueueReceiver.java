package pl.symentis.workshops.foreign.posix;

import pl.symentis.foreign.posix.errno.errno_h;
import pl.symentis.foreign.posix.mqueue.mq_attr;
import pl.symentis.foreign.posix.mqueue.mqueue_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.System.out;

/**
 * The PosixQueueReceiver class provides functionality to interact with POSIX message queues using Project Panama's Foreign Function & Memory API.
 *
 * This class implements a receiver that opens a POSIX message queue, retrieves its attributes, and waits to receive messages indefinitely.
 * Message attributes such as maximum message count and maximum message size are queried before receiving messages.
 * Additionally, detailed error handling is implemented for operations like `mq_open`, `mq_getattr`, and `mq_receive`.
 *
 * The message queue interaction includes:
 * - Creating or opening a named POSIX message queue.
 * - Querying the message queue attributes (maximum messages, maximum message size).
 * - Receiving messages from the queue in an infinite loop.
 *
 * The class extensively uses Arena for memory management and ensures proper resource deallocation.
 * It also handles native interop details such as setting or passing arguments for POSIX functions and extracting details from errno on failure.
 *
 * Note: This implementation assumes that the required external dependencies like `mqueue_h` and `errno_h` are available and correctly set up.
 */
public class PosixQueueReceiver {

    static void main() {
        try (var arena = Arena.ofConfined()) {
            var queueName = "/queue";
            var posixQueueName = arena.allocateFrom(queueName);


            // since mq_open is vararg function we need to use invoker to pass additional attributes
            // check `man mq_open` for more details
            var queue_desc = mqueue_h.mq_open.makeInvoker(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
                    .apply(posixQueueName,
                            mqueue_h.O_RDWR() | mqueue_h.O_CREAT(),
                            0664, // permission of queue
                            MemorySegment.NULL); // additional attributes

            if (queue_desc == -1) {
                int errno = errno();
                throw new RuntimeException("failed to mq_open with errno %d".formatted(errno));
            }

            var posixQueueAttributes = mq_attr.allocate(arena);
            // query actual queue attributes to get the real message size
            if (mqueue_h.mq_getattr(queue_desc, posixQueueAttributes) == -1) {
                int errno = errno();
                throw new RuntimeException("failed to mq_getattr with errno %d".formatted(errno));
            }

            out.printf("opened POSIX queue %s with attributes (max message: %d) (max message size: %d bytes)\n", queueName, mq_attr.mq_maxmsg(posixQueueAttributes), mq_attr.mq_msgsize(posixQueueAttributes));

            var messageSize = mq_attr.mq_msgsize(posixQueueAttributes);
            var msg = arena.allocate(messageSize);
            var msg_prio = arena.allocate(ValueLayout.JAVA_LONG);
            while (true) {
                var receivedBytes = mqueue_h.mq_receive(queue_desc, msg, messageSize, msg_prio);
                if (receivedBytes != -1) {
                    out.printf("Message received: %s%n", msg.getString(0));
                } else {
                    int errno = errno();
                    throw new RuntimeException("failed to mq_receive with errno " + errno);
                }
            }
        }
    }

    private static int errno() {
        return errno_h.__errno_location().get(ValueLayout.JAVA_INT, 0);
    }
}
