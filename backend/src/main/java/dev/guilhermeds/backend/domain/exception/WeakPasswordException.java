package dev.guilhermeds.backend.domain.exception;

public class WeakPasswordException extends ValidationException {

    public WeakPasswordException(int minLength) {
        super("Password must have at least " + minLength + " characters, including a letter and a digit");
    }
}
