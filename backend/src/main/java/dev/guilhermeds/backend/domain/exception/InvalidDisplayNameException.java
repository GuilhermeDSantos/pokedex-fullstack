package dev.guilhermeds.backend.domain.exception;

public class InvalidDisplayNameException extends ValidationException {

    public InvalidDisplayNameException(int minLength, int maxLength) {
        super("Display name must be between " + minLength + " and " + maxLength + " characters");
    }
}
