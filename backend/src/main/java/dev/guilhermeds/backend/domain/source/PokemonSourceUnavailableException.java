package dev.guilhermeds.backend.domain.source;

// Not a DomainException: no business rule was broken. Part of the port's contract, so it can become a 503.
public class PokemonSourceUnavailableException extends RuntimeException {

    public PokemonSourceUnavailableException(String message) {
        super(message);
    }

    public PokemonSourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
