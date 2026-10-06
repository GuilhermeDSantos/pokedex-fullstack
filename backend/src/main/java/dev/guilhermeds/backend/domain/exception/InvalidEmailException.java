package dev.guilhermeds.backend.domain.exception;

public class InvalidEmailException extends ValidationException {

    public InvalidEmailException() {
        super("Email must be a valid address");
    }
}
