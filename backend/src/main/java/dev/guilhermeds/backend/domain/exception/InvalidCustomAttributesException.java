package dev.guilhermeds.backend.domain.exception;

public class InvalidCustomAttributesException extends ValidationException {

    public InvalidCustomAttributesException(String message) {
        super(message);
    }
}
