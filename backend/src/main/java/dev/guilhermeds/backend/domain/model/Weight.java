package dev.guilhermeds.backend.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Weight(BigDecimal kilograms) {

    public Weight {
        Objects.requireNonNull(kilograms, "kilograms must not be null");
        if (kilograms.signum() < 0) {
            throw new IllegalArgumentException("weight cannot be negative");
        }
    }

    public static Weight fromHectograms(int hectograms) {
        return new Weight(BigDecimal.valueOf(hectograms).movePointLeft(1));
    }
}
