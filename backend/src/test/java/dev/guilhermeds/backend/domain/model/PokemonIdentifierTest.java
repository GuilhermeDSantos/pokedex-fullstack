package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PokemonIdentifierTest {

    @Test
    void shouldTrimAndLowerCaseWhatTheUrlCarries() {
        assertThat(new PokemonIdentifier(" Pikachu ").value()).isEqualTo("pikachu");
    }
}
