package dev.guilhermeds.backend.domain.exception;

public class InvalidPokedexNumberException extends ValidationException {

    public InvalidPokedexNumberException(int minimum) {
        super("Pokédex number must be a whole number, at least " + minimum);
    }
}
