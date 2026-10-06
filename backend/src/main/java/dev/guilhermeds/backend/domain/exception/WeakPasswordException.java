package dev.guilhermeds.backend.domain.exception;

public class WeakPasswordException extends ValidationException {

    public WeakPasswordException(int minLength, int maxLength) {
        super("Password must be " + minLength + " to " + maxLength
            + " characters long and contain a letter and a digit");
    }
}
