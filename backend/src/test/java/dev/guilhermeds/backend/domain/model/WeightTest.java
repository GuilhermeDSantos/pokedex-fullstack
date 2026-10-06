package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WeightTest {

    // PokeAPI sends weight in hectograms: Pikachu's 60 is 6.0 kg.
    @Test
    void shouldConvertHectogramsToKilograms() {
        assertThat(Weight.fromHectograms(60).kilograms()).isEqualTo(new BigDecimal("6.0"));
    }
}
