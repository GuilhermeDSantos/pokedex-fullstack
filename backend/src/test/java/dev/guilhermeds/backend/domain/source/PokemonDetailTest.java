package dev.guilhermeds.backend.domain.source;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonDetailTest {

    @Test
    void shouldRequireTheNumberProfileAndLineage() {
        var lineage = new EvolutionStage("pichu", new PokedexNumber(172), List.of());

        assertThatThrownBy(() -> new PokemonDetail(null, null, lineage)).hasMessage("number must not be null");
        assertThatThrownBy(() -> new PokemonDetail(new PokedexNumber(25), null, lineage))
            .hasMessage("profile must not be null");
    }
}
