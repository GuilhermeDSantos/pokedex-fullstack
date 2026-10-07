package dev.guilhermeds.backend.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Height(BigDecimal meters) {

    public Height {
        Objects.requireNonNull(meters, "meters must not be null");
        if (meters.signum() < 0) {
            throw new IllegalArgumentException("height cannot be negative");
        }
        meters = meters.setScale(1, RoundingMode.HALF_UP);
    }

    public static Height fromDecimetres(int decimetres) {
        return new Height(BigDecimal.valueOf(decimetres).movePointLeft(1));
    }
}
