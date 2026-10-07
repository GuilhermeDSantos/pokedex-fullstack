package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HeightTest {

    // PokeAPI sends height in decimetres: Pikachu's 4 is 0.4 m.
    @Test
    void shouldConvertDecimetresToMeters() {
        assertThat(Height.fromDecimetres(4).meters()).isEqualTo(new BigDecimal("0.4"));
    }
}
