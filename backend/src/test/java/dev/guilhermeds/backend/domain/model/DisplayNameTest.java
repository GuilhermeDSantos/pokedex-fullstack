package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidDisplayNameException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayNameTest {

    @Test
    void shouldTrimTheName() {
        assertThat(new DisplayName("  Ash Ketchum ").value()).isEqualTo("Ash Ketchum");
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 50})
    void shouldAcceptNamesAtTheLengthLimits(int length) {
        assertThat(new DisplayName("a".repeat(length)).value()).hasSize(length);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "A", " A ", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    void shouldRejectAMissingOrBadlySizedNameAsAValidationError(String raw) {
        assertThatThrownBy(() -> new DisplayName(raw))
            .isInstanceOf(InvalidDisplayNameException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("Display name must be between 2 and 50 characters");
    }
}
