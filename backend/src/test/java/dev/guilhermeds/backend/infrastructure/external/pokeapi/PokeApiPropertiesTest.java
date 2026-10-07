package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokeApiPropertiesTest {

    // A cap of zero would leave every list request waiting forever, so refuse to start instead.
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRefuseAMaxConcurrencyBelowOne(int maxConcurrency) {
        assertThatThrownBy(() -> new PokeApiProperties(URI.create("https://pokeapi.co/api/v2"), maxConcurrency))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("pokeapi.max-concurrency must be at least 1");
    }
}
