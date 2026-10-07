package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidPokedexNumberException;

public record PokedexNumber(int value) {

    public static final int MIN_VALUE = 1;

    public PokedexNumber {
        if (value < MIN_VALUE) {
            throw new InvalidPokedexNumberException(MIN_VALUE);
        }
    }

    public static PokedexNumber parse(String raw) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
