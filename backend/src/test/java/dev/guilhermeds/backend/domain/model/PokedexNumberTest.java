package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidPokedexNumberException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokedexNumberTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectANumberBelowOneAsAValidationError(int value) {
        assertThatThrownBy(() -> new PokedexNumber(value))
            .isInstanceOf(InvalidPokedexNumberException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("Pokédex number must be a whole number, at least 1");
    }

    @Test
    void shouldAcceptTheFirstPokemon() {
        assertThat(new PokedexNumber(1).value()).isEqualTo(1);
    }

    // The /local routes take a number from the URL (D-040).
    @Test
    void shouldReadANumberFromText() {
        assertThat(PokedexNumber.parse("25")).isEqualTo(new PokedexNumber(25));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "pikachu", "2.5", "-3", "0", "99999999999"})
    void shouldRejectTextThatIsNotAPokedexNumberAsAValidationError(String raw) {
        assertThatThrownBy(() -> PokedexNumber.parse(raw))
            .isInstanceOf(InvalidPokedexNumberException.class)
            .hasMessage("Pokédex number must be a whole number, at least 1");
    }
}
