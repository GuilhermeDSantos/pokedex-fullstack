package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidPokemonIdentifierException;

import java.util.Locale;
import java.util.regex.Pattern;

// What a client puts in the URL to name a Pokémon: its name or its Pokédex number.
public record PokemonIdentifier(String value) {

    private static final Pattern NAME_OR_NUMBER = Pattern.compile("^[a-z0-9-]{1,100}$");

    public PokemonIdentifier {
        value = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!NAME_OR_NUMBER.matcher(value).matches()) {
            throw new InvalidPokemonIdentifierException();
        }
    }

    public boolean isNumber() {
        return value.chars().allMatch(Character::isDigit);
    }

    // Only for a number; "0" is still rejected by PokedexNumber itself (400).
    public PokedexNumber asNumber() {
        if (!isNumber()) {
            throw new IllegalStateException("identifier is a name, not a number: " + value);
        }
        return new PokedexNumber(Integer.parseInt(value));
    }
}
