package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeightTest {

    // PokeAPI sends height in decimetres: Pikachu's 4 is 0.4 m.
    @Test
    void shouldConvertDecimetresToMeters() {
        assertThat(Height.fromDecimetres(4).meters()).isEqualTo(new BigDecimal("0.4"));
    }

    // Same rules as Weight: a negative height is a mapping bug, and 2 and 2.0 are the same height.
    @Test
    void shouldRejectANegativeHeightAndKeepOneDecimal() {
        assertThatThrownBy(() -> Height.fromDecimetres(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("height cannot be negative");
        assertThat(new Height(new BigDecimal("2"))).isEqualTo(Height.fromDecimetres(20));
    }
}
