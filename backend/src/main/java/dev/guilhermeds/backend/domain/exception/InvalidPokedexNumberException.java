package dev.guilhermeds.backend.domain.exception;

public class InvalidPokedexNumberException extends ValidationException {

    public InvalidPokedexNumberException(int minimum) {
        super("Pokédex number must be at least " + minimum);
    }
}
