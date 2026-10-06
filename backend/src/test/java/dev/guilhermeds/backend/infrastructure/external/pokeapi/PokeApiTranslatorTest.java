package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import org.junit.jupiter.api.Test;

import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.pokemon;
import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.species;
import static org.assertj.core.api.Assertions.assertThat;

class PokeApiTranslatorTest {

    private final PokeApiTranslator translator = new PokeApiTranslator();

    @Test
    void shouldTakeTheNumberAndNameFromThePokemon() {
        var summary = translator.toSummary(pokemon(25), species(25));

        assertThat(summary.number()).isEqualTo(new PokedexNumber(25));
        assertThat(summary.name()).isEqualTo("pikachu");
    }
}
