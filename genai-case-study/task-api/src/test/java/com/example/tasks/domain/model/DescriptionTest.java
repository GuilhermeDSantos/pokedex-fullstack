package com.example.tasks.domain.model;

import com.example.tasks.domain.exception.InvalidTaskException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescriptionTest {

    // An emptied field in a form means "no description", not a blank one to show.
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldTreatAMissingOrBlankDescriptionAsNone(String raw) {
        assertThat(Description.of(raw)).isNull();
    }

    @Test
    void shouldTrimTheDescriptionAndAcceptTheMaximumLength() {
        assertThat(Description.of("  Two litres ").value()).isEqualTo("Two litres");
        assertThat(Description.of("a".repeat(Description.MAX_LENGTH)).value()).hasSize(2000);
    }

    @Test
    void shouldRejectADescriptionLongerThan2000Characters() {
        assertThatThrownBy(() -> Description.of("a".repeat(2001)))
            .isInstanceOf(InvalidTaskException.class)
            .hasMessage("Description must be at most 2000 characters");
    }
}
