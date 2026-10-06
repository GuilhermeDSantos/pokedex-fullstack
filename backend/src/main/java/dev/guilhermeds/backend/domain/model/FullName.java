package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidFullNameException;

public record FullName(String value) {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 50;

    public FullName {
        if (value == null) {
            throw new InvalidFullNameException(MIN_LENGTH, MAX_LENGTH);
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidFullNameException(MIN_LENGTH, MAX_LENGTH);
        }
    }
}
