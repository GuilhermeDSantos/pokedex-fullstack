package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.ValidationException;
import dev.guilhermeds.backend.domain.exception.WeakPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawPasswordTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"pika123", "onlyletters", "1234567890"})
    void shouldRejectAWeakPasswordAsAValidationError(String raw) {
        assertThatThrownBy(() -> new RawPassword(raw))
            .isInstanceOf(WeakPasswordException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("Password must have at least 8 characters, a letter and a digit, and at most 72 bytes");
    }

    // BCrypt's limit is in bytes: these 37 characters are 73 bytes in UTF-8.
    @Test
    void shouldRejectAPasswordLongerThan72Bytes() {
        assertThatThrownBy(() -> new RawPassword("é".repeat(36) + "1"))
            .isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void shouldAcceptPasswordsAtTheLimits() {
        assertThat(new RawPassword("pikachu1").value()).hasSize(8);
        assertThat(new RawPassword("é".repeat(35) + "a1").value()).hasSize(37);
    }

    @Test
    void shouldNeverExposeThePasswordInToString() {
        assertThat(new RawPassword("pikachu1").toString()).doesNotContain("pikachu1");
    }
}
