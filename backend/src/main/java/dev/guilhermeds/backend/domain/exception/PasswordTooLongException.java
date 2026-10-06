package dev.guilhermeds.backend.domain.exception;

// The limit is 72 bytes (BCrypt), which users can't count, so the message doesn't state it.
public class PasswordTooLongException extends ValidationException {

    public PasswordTooLongException() {
        super("Password is too long");
    }
}
