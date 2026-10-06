package dev.guilhermeds.backend.domain.exception;

public class UnknownAccountException extends UnauthenticatedException {

    public UnknownAccountException() {
        super("Your session is no longer valid, please sign in again");
    }
}
