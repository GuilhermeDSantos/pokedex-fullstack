package com.example.tasks.fixture;

import com.example.tasks.application.port.UnitOfWork;

import java.util.function.Supplier;

// Runs the work at once, and counts it: an interactor test can tell whether a transaction was opened.
public final class InlineUnitOfWork implements UnitOfWork {

    private int transactions;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        transactions += 1;
        return work.get();
    }

    public int transactions() {
        return transactions;
    }
}
