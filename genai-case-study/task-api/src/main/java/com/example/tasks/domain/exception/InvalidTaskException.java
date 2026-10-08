package com.example.tasks.domain.exception;

public class InvalidTaskException extends ValidationException {

    public InvalidTaskException(String message) {
        super(message);
    }
}
