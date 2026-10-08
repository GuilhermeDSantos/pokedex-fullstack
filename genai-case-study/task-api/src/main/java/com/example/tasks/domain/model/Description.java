package com.example.tasks.domain.model;

public record Description(String value) {

    public static final int MAX_LENGTH = 2000;

    public static Description of(String raw) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
