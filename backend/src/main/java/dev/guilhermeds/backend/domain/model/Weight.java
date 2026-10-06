package dev.guilhermeds.backend.domain.model;

import java.math.BigDecimal;

public record Weight(BigDecimal kilograms) {

    public static Weight fromHectograms(int hectograms) {
        return new Weight(BigDecimal.valueOf(hectograms).movePointLeft(1));
    }
}
