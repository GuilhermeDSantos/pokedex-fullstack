package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidEmailException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void shouldTrimAndLowerCaseTheAddress() {
        assertThat(new Email("  Ash.Ketchum@Pallet.Town ").value()).isEqualTo("ash.ketchum@pallet.town");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "ash", "ash@", "@pallet.town", "ash@pallet", "ash ketchum@pallet.town", "ash@@pallet.town"})
    void shouldRejectAnInvalidAddressAsAValidationError(String raw) {
        assertThatThrownBy(() -> new Email(raw))
            .isInstanceOf(InvalidEmailException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("Email must be a valid address");
    }

    @Test
    void shouldRejectAnAddressLongerThan254Characters() {
        var tooLong = "a".repeat(245) + "@pallet.town";

        assertThatThrownBy(() -> new Email(tooLong)).isInstanceOf(InvalidEmailException.class);
    }
}
