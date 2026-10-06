package dev.guilhermeds.backend.domain.model;

// Comes from PokeAPI, not from a user: a blank one is a mapping bug, not a 400.
public record PokemonType(String name) {

    public PokemonType {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("type name must not be blank");
        }
    }
}
