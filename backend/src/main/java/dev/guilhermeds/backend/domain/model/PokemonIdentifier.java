package dev.guilhermeds.backend.domain.model;

import java.util.Locale;

// What a client puts in the URL to name a Pokémon: its name or its Pokédex number.
public record PokemonIdentifier(String value) {

    public PokemonIdentifier {
        value = value.trim().toLowerCase(Locale.ROOT);
    }
}
