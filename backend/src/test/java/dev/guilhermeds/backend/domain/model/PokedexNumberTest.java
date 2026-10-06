package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidPokedexNumberException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokedexNumberTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectANumberBelowOneAsAValidationError(int value) {
        assertThatThrownBy(() -> new PokedexNumber(value))
            .isInstanceOf(InvalidPokedexNumberException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("Pokédex number must be at least 1");
    }
}
