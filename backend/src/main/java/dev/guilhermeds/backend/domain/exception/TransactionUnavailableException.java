package dev.guilhermeds.backend.domain.exception;

public class TransactionUnavailableException extends DataUnavailableException {

    public TransactionUnavailableException(Throwable cause) {
        super("A transaction couldn't be started", cause);
    }
}
