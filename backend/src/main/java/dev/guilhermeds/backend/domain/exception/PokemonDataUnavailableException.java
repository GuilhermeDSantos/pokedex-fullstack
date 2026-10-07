package dev.guilhermeds.backend.domain.exception;

public class PokemonDataUnavailableException extends DataUnavailableException {

    public PokemonDataUnavailableException(String message) {
        super(message);
    }

    public PokemonDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
