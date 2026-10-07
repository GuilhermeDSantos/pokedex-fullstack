package dev.guilhermeds.backend.domain.model;

import java.util.Locale;

// An internal classification tag (US-03.a).
public record Tag(String value) {

    public Tag {
        value = value.trim().toLowerCase(Locale.ROOT);
    }
}
