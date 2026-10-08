package com.example.tasks.domain.model;

import com.example.tasks.domain.exception.InvalidTaskException;

public record Description(String value) {

    public static final int MAX_LENGTH = 2000;

    public Description {
        if (value == null || value.isBlank()) {
            throw new InvalidTaskException("Description must not be blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidTaskException("Description must be at most " + MAX_LENGTH + " characters");
        }
    }

    // Optional on a task: blank means "no description", so there's nothing to keep.
    public static Description of(String raw) {
        return raw == null || raw.isBlank() ? null : new Description(raw);
    }
}
