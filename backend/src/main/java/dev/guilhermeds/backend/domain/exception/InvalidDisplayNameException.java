package dev.guilhermeds.backend.domain.exception;

public class InvalidDisplayNameException extends ValidationException {

    public InvalidDisplayNameException() {
        super("Display name must be between 2 and 50 characters");
    }
}
