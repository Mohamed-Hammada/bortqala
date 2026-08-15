package com.bemo.shared.util;

/**
 * Turns checked-exception lambdas into unchecked ones. Use sparingly at boundaries where the
 * checked exception is guaranteed not to occur (e.g. reading a hard-coded resource).
 */
public final class Suppress {

    private Suppress() {
    }

    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }

    public static <T> T unchecked(SupplierWithException<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return sneakyThrow(e);
        }
    }

    public static void unchecked(RunnableWithException runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            sneakyThrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }
}
