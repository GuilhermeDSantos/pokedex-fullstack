package dev.guilhermeds.backend.domain.exception;

import dev.guilhermeds.backend.domain.model.PokedexNumber;

public class LocalPokemonNotFoundException extends NotFoundException {

    public LocalPokemonNotFoundException(PokedexNumber number) {
        super("Pokémon #" + number.value() + " is not in the local database");
    }
}
