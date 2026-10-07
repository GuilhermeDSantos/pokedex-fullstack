package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidPokemonIdentifierException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonIdentifierTest {

    @Test
    void shouldTrimAndLowerCaseWhatTheUrlCarries() {
        assertThat(new PokemonIdentifier(" Pikachu ").value()).isEqualTo("pikachu");
    }

    // Names are PokeAPI slugs (letters, digits, hyphens); numbers are digits. Anything else is a 400.
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "pika chu", "pikachu!", "../admin"})
    void shouldRejectWhatIsNeitherANameNorANumber(String value) {
        assertThatThrownBy(() -> new PokemonIdentifier(value))
            .isInstanceOf(InvalidPokemonIdentifierException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("A Pokémon is identified by its name or its Pokédex number");
    }
}
