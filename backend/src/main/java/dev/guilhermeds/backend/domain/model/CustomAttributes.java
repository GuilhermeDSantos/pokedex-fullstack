package dev.guilhermeds.backend.domain.model;

import java.util.Set;

// The proprietary fields of the brief: localized name, region, internal tags. Ours, unlike the canonical data.
public record CustomAttributes(String localizedName, String region, Set<Tag> tags) {

    public static final int MAX_TEXT_LENGTH = 100;

    public CustomAttributes {
        localizedName = trimToNull(localizedName);
        region = trimToNull(region);
    }

    public static CustomAttributes empty() {
        return new CustomAttributes(null, null, Set.of());
    }

    private static String trimToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
