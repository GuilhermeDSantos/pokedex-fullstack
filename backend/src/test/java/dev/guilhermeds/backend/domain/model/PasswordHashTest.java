package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRejectAMissingHash(String raw) {
        assertThatThrownBy(() -> new PasswordHash(raw)).isInstanceOf(IllegalArgumentException.class);
    }
}
