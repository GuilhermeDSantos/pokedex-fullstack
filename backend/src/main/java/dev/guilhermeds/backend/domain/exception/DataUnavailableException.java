package dev.guilhermeds.backend.domain.exception;

// Not a DomainException: no business rule was broken, some data just can't be reached right now.
// Each repository has its own subclass, so the log says which one and why.
public abstract class DataUnavailableException extends RuntimeException {

    protected DataUnavailableException(String message) {
        super(message);
    }

    protected DataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
