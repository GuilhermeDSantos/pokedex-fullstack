package dev.guilhermeds.backend.domain.exception;

public abstract class UnauthenticatedException extends DomainException {

    protected UnauthenticatedException(String message) {
        super(message);
    }
}
