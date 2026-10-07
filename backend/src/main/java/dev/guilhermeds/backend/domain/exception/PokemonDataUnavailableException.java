package dev.guilhermeds.backend.domain.exception;

// Not a DomainException: no business rule was broken. Part of the port's contract, so it can become a 503.
public class PokemonDataUnavailableException extends RuntimeException {

    public PokemonDataUnavailableException(String message) {
        super(message);
    }

    public PokemonDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
