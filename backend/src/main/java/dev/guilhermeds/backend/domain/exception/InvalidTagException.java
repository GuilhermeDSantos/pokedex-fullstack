package dev.guilhermeds.backend.domain.exception;

public class InvalidTagException extends ValidationException {

    public InvalidTagException(int maxLength) {
        super("A tag uses only letters, digits and hyphens, starts with a letter or a digit, and has at most "
            + maxLength + " characters");
    }
}
