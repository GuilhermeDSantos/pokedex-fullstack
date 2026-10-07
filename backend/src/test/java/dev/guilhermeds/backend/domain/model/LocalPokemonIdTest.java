package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPokemonIdTest {

    @Test
    void shouldRequireAValue() {
        assertThatThrownBy(() -> new LocalPokemonId(null)).hasMessage("local Pokémon id must not be null");
    }

    @Test
    void shouldGenerateADifferentIdEachTime() {
        assertThat(LocalPokemonId.generate()).isNotEqualTo(LocalPokemonId.generate());
    }
}
