# Java Foreign Function & Memory API Workshop

This workshop project demonstrates Java's Foreign Function & Memory API (Project Panama) using Java 25. It focuses on interoperating with native C libraries, specifically POSIX message queues.

## Prerequisites

- **Java 25** (Temurin 25+36-LTS)
- **Maven 3.9.11+**
- **Task** (Taskfile runner) - [Installation instructions](https://taskfile.dev/installation/)
- **direnv** (optional, for automatic environment setup)

## Quick Start

### 1. Environment Setup

If using direnv and SDKMAN:
```bash
direnv allow  # Automatically activates Java 25 and Maven 3.9.11
```

Or manually:
```bash
sdk use java 25-tem
sdk use maven 3.9.11
```

### 2. Build the Project

```bash
task  # Downloads jextract and builds all modules
```

## Running POSIX Queue Examples

The `foreign-posix-queue` module includes two example programs that demonstrate inter-process communication using POSIX message queues.

### Running the Receiver

In one terminal, start the receiver process:

```bash
task run-posix-receiver
```

The receiver will:
- Open/create a POSIX message queue named `/queue`
- Display queue attributes (max messages, message size)
- Wait for incoming messages
- Print each received message to stdout

### Running the Sender

In another terminal, start the sender process:

```bash
task run-posix-sender
```

The sender will:
- Open the same POSIX message queue (`/queue`)
- Prompt you to type messages
- Send each line you type to the receiver
- Exit when you press Ctrl+D or Ctrl+C

### Example Session

**Terminal 1 (Receiver):**
```
$ task run-posix-receiver
opened POSIX queue /queue with attributes (max message: 10) (max message size: 8192 bytes)
Message received: Hello from sender!
Message received: Testing POSIX queues
Message received: This is awesome!
```

**Terminal 2 (Sender):**
```
$ task run-posix-sender
Opened POSIX queue /queue for sending messages
Type messages and press Enter to send (Ctrl+D or Ctrl+C to exit):
Hello from sender!
Message sent: Hello from sender!
Testing POSIX queues
Message sent: Testing POSIX queues
This is awesome!
Message sent: This is awesome!
^D
Sender shutting down...
```

## Troubleshooting

### jextract not found

If you see errors about jextract not being found:
```bash
task setup-jextract  # Downloads jextract to .local/
```

### Queue already exists

POSIX queues persist across process restarts. To clean up:
```bash
# Remove the queue manually
rm /dev/mqueue/queue
```

### Permission denied

Ensure you have permissions to create/access POSIX queues on your system.

## Learning Resources

- [JEP 454: Foreign Function & Memory API](https://openjdk.org/jeps/454)
- [Project Panama Documentation](https://openjdk.org/projects/panama/)
- [jextract Guide](https://github.com/openjdk/jextract)
- [POSIX Message Queues Manual](https://man7.org/linux/man-pages/man7/mq_overview.7.html)