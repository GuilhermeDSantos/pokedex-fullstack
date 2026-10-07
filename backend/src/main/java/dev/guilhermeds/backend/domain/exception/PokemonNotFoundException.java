package dev.guilhermeds.backend.domain.exception;

import dev.guilhermeds.backend.domain.model.PokemonIdentifier;

public class PokemonNotFoundException extends NotFoundException {

    public PokemonNotFoundException(PokemonIdentifier identifier) {
        super("Pokémon '" + identifier.value() + "' was not found");
    }
}
