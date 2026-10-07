package dev.guilhermeds.backend.domain.exception;

import dev.guilhermeds.backend.domain.model.PokedexNumber;

public class LocalPokemonModifiedConcurrentlyException extends ConflictException {

    public LocalPokemonModifiedConcurrentlyException(PokedexNumber number) {
        super("Pokémon #" + number.value() + " was changed by someone else in the meantime. Reload it and try again");
    }
}
