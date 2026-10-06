package dev.guilhermeds.backend.domain.exception;

public class InvalidCredentialsException extends UnauthenticatedException {

    // Same message for an unknown email and a wrong password, so emails can't be enumerated.
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
