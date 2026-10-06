package dev.guilhermeds.backend.domain.exception;

public class WeakPasswordException extends ValidationException {

    public WeakPasswordException() {
        super("Password must be 8 to 72 characters long and contain a letter and a digit");
    }
}
