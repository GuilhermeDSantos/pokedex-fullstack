package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.WeakPasswordException;

import java.nio.charset.StandardCharsets;

public record RawPassword(String value) {

    public static final int MIN_LENGTH = 8;
    // BCrypt rejects inputs longer than 72 bytes, so the limit is in bytes, not characters.
    public static final int MAX_BYTES = 72;

    public RawPassword {
        if (value == null
            || value.length() < MIN_LENGTH
            || value.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES
            || value.chars().noneMatch(Character::isLetter)
            || value.chars().noneMatch(Character::isDigit)) {
            throw new WeakPasswordException();
        }
    }

    @Override
    public String toString() {
        return "RawPassword[****]";
    }
}
