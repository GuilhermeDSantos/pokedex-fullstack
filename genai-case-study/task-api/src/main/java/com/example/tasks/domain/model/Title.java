package com.example.tasks.domain.model;

import com.example.tasks.domain.exception.InvalidTaskException;

public record Title(String value) {

    public static final int MAX_LENGTH = 200;

    public Title {
        if (value == null || value.isBlank()) {
            throw new InvalidTaskException("Title is required");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidTaskException("Title must be at most " + MAX_LENGTH + " characters");
        }
    }
}
