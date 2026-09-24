/*
 * Pure C equivalent of pl.symentis.workshops.foreign.posix.PosixQueueSender.
 *
 * Opens the same "/queue" POSIX message queue used by the Java examples and
 * sends whatever lines are typed on stdin, demonstrating that the queue is
 * just an OS-level rendezvous point that C and Java processes can share.
 */
#include <errno.h>
#include <fcntl.h>
#include <mqueue.h>
#include <stdio.h>
#include <string.h>

int main(void) {
    const char *queue_name = "/queue";

    mqd_t queue_desc = mq_open(queue_name, O_WRONLY | O_CREAT, 0664, NULL);
    if (queue_desc == (mqd_t) -1) {
        fprintf(stderr, "failed to mq_open with errno %d (%s)\n", errno, strerror(errno));
        return 1;
    }

    printf("Opened POSIX queue %s for sending messages\n", queue_name);
    printf("Type messages and press Enter to send (Ctrl+D or Ctrl+C to exit):\n");

    char message[8192];
    while (fgets(message, sizeof(message), stdin) != NULL) {
        size_t length = strlen(message);
        if (length > 0 && message[length - 1] == '\n') {
            message[--length] = '\0';
        }

        if (length == 0) {
            continue;
        }

        if (mq_send(queue_desc, message, length + 1, 0) == -1) {
            fprintf(stderr, "failed to mq_send with errno %d (%s)\n", errno, strerror(errno));
            mq_close(queue_desc);
            return 1;
        }

        printf("Message sent: %s\n", message);
    }

    if (mq_close(queue_desc) == -1) {
        fprintf(stderr, "Warning: failed to mq_close with errno %d (%s)\n", errno, strerror(errno));
    }

    printf("Sender shutting down...\n");
    return 0;
}
