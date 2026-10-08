package com.example.tasks.domain.exception;

// A broken business rule. The REST layer maps each abstract category to one HTTP status.
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
