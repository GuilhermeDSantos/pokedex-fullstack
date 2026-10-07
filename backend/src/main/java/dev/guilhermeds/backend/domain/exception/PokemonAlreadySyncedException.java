package dev.guilhermeds.backend.domain.exception;

import dev.guilhermeds.backend.domain.model.PokedexNumber;

public class PokemonAlreadySyncedException extends ConflictException {

    public PokemonAlreadySyncedException(PokedexNumber number) {
        super("Pokémon #" + number.value() + " is already in the local database");
    }
}
