package dev.guilhermeds.backend.domain.exception;

public class InvalidFullNameException extends ValidationException {

    public InvalidFullNameException(int minLength, int maxLength) {
        super("Name must be between " + minLength + " and " + maxLength + " characters");
    }
}
