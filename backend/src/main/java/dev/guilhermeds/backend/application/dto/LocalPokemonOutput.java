package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.Tag;

import java.time.Instant;
import java.util.List;

// Our record alone: the Pokédex number and our own fields. Names come with the canonical data.
public record LocalPokemonOutput(
    int pokedexNumber,
    String localizedName,
    String region,
    List<String> tags,
    Instant syncedAt,
    Instant updatedAt
) {

    public static LocalPokemonOutput from(LocalPokemon pokemon) {
        var custom = pokemon.getCustomAttributes();
        return new LocalPokemonOutput(
            pokemon.getPokedexNumber().value(),
            custom.localizedName(),
            custom.region(),
            custom.tags().stream().map(Tag::value).sorted().toList(),
            pokemon.getSyncedAt(),
            pokemon.getUpdatedAt());
    }
}
