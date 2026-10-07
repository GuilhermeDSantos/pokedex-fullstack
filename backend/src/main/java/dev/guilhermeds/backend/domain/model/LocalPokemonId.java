package dev.guilhermeds.backend.domain.model;

import java.util.Objects;
import java.util.UUID;

public record LocalPokemonId(UUID value) {

    public LocalPokemonId {
        Objects.requireNonNull(value, "local Pokémon id must not be null");
    }

    // Called only at the edge (interfaces/), so use cases receive the id instead of inventing it.
    public static LocalPokemonId generate() {
        return new LocalPokemonId(UUID.randomUUID());
    }
}
