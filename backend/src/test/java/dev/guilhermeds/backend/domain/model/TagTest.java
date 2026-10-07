package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidTagException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagTest {

    // "Starter" and "starter " are the same classification, so they are stored the same way.
    @Test
    void shouldTrimAndLowerCaseTheTag() {
        assertThat(new Tag("  Starter ").value()).isEqualTo("starter");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "-starter", "has space", "under_score", "ção", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void shouldRejectATagOutsideTheFormatAsAValidationError(String raw) {
        assertThatThrownBy(() -> new Tag(raw))
            .isInstanceOf(InvalidTagException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("A tag uses only letters, digits and hyphens, starts with a letter or a digit, and has at most 30 characters");
    }
}
