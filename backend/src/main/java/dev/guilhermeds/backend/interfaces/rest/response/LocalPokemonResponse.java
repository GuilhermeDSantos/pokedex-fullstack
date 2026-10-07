package dev.guilhermeds.backend.interfaces.rest.response;

import java.time.Instant;
import java.util.List;

public record LocalPokemonResponse(
    int pokedexNumber,
    String localizedName,
    String region,
    List<String> tags,
    Instant syncedAt,
    Instant updatedAt
) {
}
