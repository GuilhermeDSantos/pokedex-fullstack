package dev.guilhermeds.backend.domain.exception;

public class LocalPokemonDataUnavailableException extends DataUnavailableException {

    public LocalPokemonDataUnavailableException(Throwable cause) {
        super("Local Pokémon records can't be reached", cause);
    }
}
