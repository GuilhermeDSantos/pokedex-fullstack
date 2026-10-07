package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidTagException;

import java.util.Locale;
import java.util.regex.Pattern;

// An internal classification tag (US-03.a).
public record Tag(String value) {

    public static final int MAX_LENGTH = 30;

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9][a-z0-9-]{0," + (MAX_LENGTH - 1) + "}");

    public Tag {
        if (value == null) {
            throw new InvalidTagException(MAX_LENGTH);
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidTagException(MAX_LENGTH);
        }
    }
}
