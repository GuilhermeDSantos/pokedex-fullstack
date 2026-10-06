package dev.guilhermeds.backend.domain.exception;

public class WeakPasswordException extends ValidationException {

    public WeakPasswordException(int minLength, int maxBytes) {
        super("Password must have at least " + minLength + " characters, a letter and a digit, and at most "
            + maxBytes + " bytes");
    }
}
