package dev.guilhermeds.backend.domain.exception;

public class InvalidPageRequestException extends ValidationException {

    public InvalidPageRequestException(String message) {
        super(message);
    }
}
