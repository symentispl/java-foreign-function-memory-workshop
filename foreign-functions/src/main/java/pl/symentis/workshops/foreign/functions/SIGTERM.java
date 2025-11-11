package pl.symentis.workshops.foreign.functions;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.System.out;

/**
 * The {@code SIGTERM} class demonstrates the usage of the Foreign Function & Memory API in Java
 * to interact with native code, specifically handling UNIX signals such as SIGTERM.
 * <p>
 * This class uses the Foreign Linker API to:
 * - Look up symbols in the native environment (e.g., the "signal" function).
 * - Create a signal handler as a Java method that conforms to a native function signature.
 * - Map the Java signal handler method to a native signal handling routine.
 * <p>
 * The {@code main} method sets up the environment, installs a signal handler for a specific signal
 * (e.g., signal 15, SIGTERM), and waits for a signal or user interaction to exit.
 * <p>
 * Features:
 * - The {@link Linker} and {@link MethodHandles} API are used to interact with the signal mechanism.
 * - Demonstrates upcall stubs to map a Java-defined signal handling method to a native environment.
 * - Utilizes the {@link Arena#global()} to manage session memory during native interaction.
 * <p>
 * How to run:
 * - Run this class from the command line.
 * - Find its pid
 * - Run kill -15 [pid] to invoke the signal handler
 * <p>
 * Note:
 * This example is specific to environments where the "signal" function is available, typically on UNIX-like systems.
 * Ensure the appropriate platform support and API understanding when using this class.
 */
public class SIGTERM {

    static void main() throws Throwable {
        var linker = Linker.nativeLinker();
        var symbolLookup = linker.defaultLookup();

        out.println("creating signal handler stub");
        // QUESTION: why the hell global scope?
        var globalSession = Arena.global();
        var signalHandler = MethodHandles.lookup()
                .findStatic(SIGTERM.class, "onSignal", MethodType.methodType(void.class, int.class));
        var signalHandlerStub =
                linker.upcallStub(signalHandler, FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT), globalSession);

        out.println("installing signal handler " + signalHandlerStub);
        var signal = symbolLookup.find("signal").orElseThrow(() -> new RuntimeException("signal symbol not found"));
        var signalHandle =
                linker.downcallHandle(signal, FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        out.println(signalHandle.type());
        signalHandle.invoke(15, signalHandlerStub);

        out.println("Press <ENTER> to quit");
        System.in.read();
    }

    public static void onSignal(int signal) {
        out.printf("Received signal %d in thread %s\n", signal, Thread.currentThread());
    }
}
