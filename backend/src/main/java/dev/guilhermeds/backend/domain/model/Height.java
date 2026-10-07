package dev.guilhermeds.backend.domain.model;

import java.math.BigDecimal;

public record Height(BigDecimal meters) {

    public static Height fromDecimetres(int decimetres) {
        return new Height(BigDecimal.valueOf(decimetres).movePointLeft(1));
    }
}
