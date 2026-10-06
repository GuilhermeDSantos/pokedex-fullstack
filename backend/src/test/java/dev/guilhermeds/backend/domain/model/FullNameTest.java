package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidFullNameException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FullNameTest {

    @Test
    void shouldTrimTheName() {
        assertThat(new FullName("  Ash Ketchum ").value()).isEqualTo("Ash Ketchum");
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 100})
    void shouldAcceptNamesAtTheLengthLimits(int length) {
        assertThat(new FullName("a".repeat(length)).value()).hasSize(length);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "A", " A ", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    void shouldRejectAMissingOrBadlySizedNameAsAValidationError(String raw) {
        assertThatThrownBy(() -> new FullName(raw))
            .isInstanceOf(InvalidFullNameException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("Name must be between 2 and 100 characters");
    }
}
