package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonTypeTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void shouldRejectABlankName(String name) {
        assertThatThrownBy(() -> new PokemonType(name))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("type name must not be blank");
    }

    @Test
    void shouldNormalizeTheName() {
        assertThat(new PokemonType(" Electric ").name()).isEqualTo("electric");
    }
}
