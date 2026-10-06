package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbilityTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void shouldRejectABlankName(String name) {
        assertThatThrownBy(() -> new Ability(name, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ability name must not be blank");
    }
}
