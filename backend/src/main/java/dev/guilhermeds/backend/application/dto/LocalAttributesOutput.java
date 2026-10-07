package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.Tag;

import java.time.Instant;
import java.util.List;

// Our part of a merged Pokémon (D-030).
public record LocalAttributesOutput(String localizedName, String region, List<String> tags, Instant syncedAt,
                                    Instant updatedAt) {

    public static LocalAttributesOutput from(LocalPokemon pokemon) {
        var custom = pokemon.getCustomAttributes();
        return new LocalAttributesOutput(custom.localizedName(), custom.region(),
            custom.tags().stream().map(Tag::value).sorted().toList(), pokemon.getSyncedAt(), pokemon.getUpdatedAt());
    }
}
