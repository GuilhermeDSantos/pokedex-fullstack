package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidCustomAttributesException;

import java.util.Set;

// The proprietary fields of the brief: localized name, region, internal tags. Ours, unlike the canonical data.
public record CustomAttributes(
    String localizedName,
    String region,
    Set<Tag> tags
) {

    public static final int MAX_TEXT_LENGTH = 100;
    public static final int MAX_TAGS = 10;

    public CustomAttributes {
        localizedName = optionalText(localizedName, "Localized name");
        region = optionalText(region, "Region");
        tags = Set.copyOf(tags);
        if (tags.size() > MAX_TAGS) {
            throw new InvalidCustomAttributesException("A Pokémon has at most " + MAX_TAGS + " tags");
        }
    }

    public static CustomAttributes empty() {
        return new CustomAttributes(null, null, Set.of());
    }

    private static String optionalText(String text, String field) {
        if (text == null || text.isBlank()) {
            return null;
        }
        var trimmed = text.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new InvalidCustomAttributesException(field + " must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        return trimmed;
    }
}
