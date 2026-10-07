package dev.guilhermeds.backend.domain.source;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvolutionStageTest {

    @Test
    void shouldRequireTheSpeciesNameAndNumber() {
        assertThatThrownBy(() -> new EvolutionStage(null, new PokedexNumber(133), List.of()))
            .hasMessage("speciesName must not be null");
        assertThatThrownBy(() -> new EvolutionStage("eevee", null, List.of()))
            .hasMessage("number must not be null");
    }
}
