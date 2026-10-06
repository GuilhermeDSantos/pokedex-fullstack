package dev.guilhermeds.backend.domain.exception;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException() {
        super("This email is already registered");
    }
}
