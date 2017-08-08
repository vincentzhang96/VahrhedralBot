package co.phoenixlab.discord.util;

import java.util.function.Consumer;

public class SneakyThrows {

    /**
     * Throw even checked exceptions without being required
     * to declare them or catch them. Suggested idiom:
     * throw sneakyThrow( some exception );
     */
    static public RuntimeException sneakyThrow(Throwable t) {
        // http://www.mail-archive.com/javaposse@googlegroups.com/msg05984.html
        if (t == null)
            throw new NullPointerException();
        sneakyThrow0(t);
        return (RuntimeException) t;
    }

    @SuppressWarnings("unchecked")
    static private <T extends Throwable> void sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }

    public static <T> Consumer<T> sneaky(VoidInstanceThrower<T, ? extends Exception> thrower) {
        return sneaky(thrower, SneakyThrows::sneakyThrow);
    }

    public static <T> Consumer<T> sneaky(VoidInstanceThrower<T, ? extends Exception> thrower, Consumer<Exception> onFail) {
        return (T t) -> {
            try {
                thrower.run(t);
            } catch (Exception e) {
                onFail.accept(e);
            }
        };
    }

    public static Runnable sneaky(VoidThrower<? extends Exception> thrower) {
        return sneaky(thrower, SneakyThrows::sneakyThrow);
    }

    public static Runnable sneaky(VoidThrower<? extends Exception> thrower, Consumer<Exception> onFail) {
        return () -> {
            try {
                thrower.run();
            } catch (Exception e) {
                onFail.accept(e);
            }
        };
    }

    public interface VoidThrower<T extends Throwable> {
        void run() throws T;
    }

    public interface VoidInstanceThrower<V, T extends Throwable> {
        void run(V v) throws T;
    }
}
