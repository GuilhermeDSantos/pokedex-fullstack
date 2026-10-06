package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidDisplayNameException;

public record DisplayName(String value) {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 50;

    public DisplayName {
        if (value == null) {
            throw new InvalidDisplayNameException();
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidDisplayNameException();
        }
    }
}
