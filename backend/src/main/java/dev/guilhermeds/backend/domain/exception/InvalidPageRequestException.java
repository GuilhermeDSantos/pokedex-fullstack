package dev.guilhermeds.backend.domain.exception;

public class InvalidPageRequestException extends ValidationException {

    private InvalidPageRequestException(String message) {
        super(message);
    }

    public static InvalidPageRequestException negativePage() {
        return new InvalidPageRequestException("Page cannot be negative");
    }

    public static InvalidPageRequestException sizeOutOfRange(int minSize, int maxSize) {
        return new InvalidPageRequestException("Size must be between " + minSize + " and " + maxSize);
    }
}
