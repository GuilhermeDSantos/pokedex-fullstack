package com.example.tasks.domain.exception;

public class InvalidPageRequestException extends ValidationException {

    public InvalidPageRequestException(String message) {
        super(message);
    }
}
