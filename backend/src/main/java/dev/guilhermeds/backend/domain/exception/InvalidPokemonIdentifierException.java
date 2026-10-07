package dev.guilhermeds.backend.domain.exception;

public class InvalidPokemonIdentifierException extends ValidationException {

    public InvalidPokemonIdentifierException() {
        super("A Pokémon is identified by its name or its Pokédex number");
    }
}
