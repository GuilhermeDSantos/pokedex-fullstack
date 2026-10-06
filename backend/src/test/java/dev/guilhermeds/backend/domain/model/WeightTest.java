package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeightTest {

    // PokeAPI sends weight in hectograms: Pikachu's 60 is 6.0 kg.
    @Test
    void shouldConvertHectogramsToKilograms() {
        assertThat(Weight.fromHectograms(60).kilograms()).isEqualTo(new BigDecimal("6.0"));
    }

    // A negative weight can only come from a broken mapping, so it's a bug, not a 400.
    @Test
    void shouldRejectANegativeWeight() {
        assertThatThrownBy(() -> Weight.fromHectograms(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("weight cannot be negative");
    }
}
