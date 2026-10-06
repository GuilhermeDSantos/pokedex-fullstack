package dev.guilhermeds.backend.application.port;

import java.util.function.Supplier;

public interface UnitOfWork {

    <T> T inTransaction(Supplier<T> work);

    // Call both overloads with a block lambda: a bare expression matches both and won't compile.
    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
