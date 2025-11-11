package pl.symentis.workshops.foreign.functions;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;

/**
 * The {@code GetPIDExample} class demonstrates how to use the Foreign Function & Memory API in Java
 * to interact with native libraries for retrieving the process identifier (PID) of the current application.
 *
 * This class utilizes the {@link Linker} and {@link FunctionDescriptor} to:
 * - Dynamically look up the "getpid" symbol in the native environment.
 * - Create a {@link java.lang.invoke.MethodHandle} for invoking the native "getpid" function.
 *
 * Features:
 * - Demonstrates the use of the Foreign Linker API to interact with a native function.
 * - Uses the {@code FunctionDescriptor} to specify the function's expected return type and parameters.
 * - Demonstrates symbol lookup and dynamic invocation of a native function to retrieve the process ID.
 *
 * Notes:
 * This example assumes the presence of a "getpid" function in the underlying native platform, typically available on UNIX-like systems.
 * Ensure that the runtime environment has support for the required function.
 */
public class GetPIDExample {

    static void main() throws Throwable {
        var linker = Linker.nativeLinker();
        var symbolLookup = linker.defaultLookup();

        var getpidSymbol =
                symbolLookup.find("getpid").orElseThrow(() -> new RuntimeException("getpid symbol not found"));

        var functionDescriptor = FunctionDescriptor.of(ValueLayout.JAVA_INT);

        var methodHandle = linker.downcallHandle(getpidSymbol, functionDescriptor);
        System.out.println("PID: "+methodHandle.invoke());
    }
}
