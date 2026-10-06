package dev.guilhermeds.backend.domain.exception;

public class InvalidFullNameException extends ValidationException {

    public InvalidFullNameException(int minLength, int maxLength) {
        super("Display name must be between " + minLength + " and " + maxLength + " characters");
    }
}
