package dev.guilhermeds.backend.domain.model;

// Produced by the PasswordHasher, never typed by a user: a blank one is a bug, not a 400.
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("password hash must not be blank");
        }
    }
}
