package dev.guilhermeds.backend.domain.exception;

public class UserAccountDataUnavailableException extends DataUnavailableException {

    public UserAccountDataUnavailableException(Throwable cause) {
        super("User accounts can't be reached", cause);
    }
}
