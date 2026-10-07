package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidPokedexNumberException;

import java.util.regex.Pattern;

public record PokedexNumber(int value) {

    public static final int MIN_VALUE = 1;

    private static final Pattern DIGITS = Pattern.compile("[0-9]+");

    public PokedexNumber {
        if (value < MIN_VALUE) {
            throw new InvalidPokedexNumberException(MIN_VALUE);
        }
    }

    public static PokedexNumber parse(String raw) {
        if (raw == null || !DIGITS.matcher(raw).matches()) {
            throw new InvalidPokedexNumberException(MIN_VALUE);
        }
        try {
            return new PokedexNumber(Integer.parseInt(raw));
        } catch (NumberFormatException tooLargeForAnInt) {
            throw new InvalidPokedexNumberException(MIN_VALUE);
        }
    }
}
