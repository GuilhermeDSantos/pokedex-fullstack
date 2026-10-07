package dev.guilhermeds.backend.domain.model;

import java.util.Objects;

public record BaseStat(StatName name, int value) {

    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 255;

    public BaseStat {
        Objects.requireNonNull(name, "name must not be null");
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException("a base stat runs from " + MIN_VALUE + " to " + MAX_VALUE);
        }
    }
}
