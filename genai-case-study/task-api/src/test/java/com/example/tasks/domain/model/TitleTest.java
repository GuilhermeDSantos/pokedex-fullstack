package com.example.tasks.domain.model;

import com.example.tasks.domain.exception.InvalidTaskException;
import com.example.tasks.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TitleTest {

    @Test
    void shouldTrimTheTitle() {
        assertThat(new Title("  Buy milk ").value()).isEqualTo("Buy milk");
    }

    @Test
    void shouldAcceptATitleOfTheMaximumLength() {
        assertThat(new Title("a".repeat(Title.MAX_LENGTH)).value()).hasSize(200);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRejectAMissingTitleAsAValidationError(String raw) {
        assertThatThrownBy(() -> new Title(raw))
            .isInstanceOf(InvalidTaskException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("Title is required");
    }

    @Test
    void shouldRejectATitleLongerThan200Characters() {
        assertThatThrownBy(() -> new Title("a".repeat(201)))
            .isInstanceOf(InvalidTaskException.class)
            .hasMessage("Title must be at most 200 characters");
    }
}
