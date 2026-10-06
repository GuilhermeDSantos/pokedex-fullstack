package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.Weight;
import java.util.List;
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

    // PokeAPI sends hectograms: Pikachu's 60 is 6.0 kg, the brief's "mass in kg".
    @Test
    void shouldConvertTheWeightToKilograms() {
        assertThat(translator.toSummary(pokemon(25), species(25)).weight()).isEqualTo(Weight.fromHectograms(60));
        assertThat(translator.toSummary(pokemon(25), species(25)).weight().kilograms()).isEqualByComparingTo("6.0");
    }

    // The category is the species' English genus (D-010); genera holds one entry per language.
    @Test
    void shouldUseTheEnglishGenusAsTheCategory() {
        assertThat(translator.toSummary(pokemon(25), species(25)).category()).isEqualTo("Mouse Pokémon");
    }

    @Test
    void shouldLeaveTheCategoryEmptyWhenThereIsNoEnglishGenus() {
        var onlyFrench = new PokeApiSpeciesJson(
            List.of(new PokeApiSpeciesJson.Genus("Pokémon Souris", new NamedResource("fr", "https://pokeapi.co/api/v2/language/5/"))));

        assertThat(translator.toSummary(pokemon(25), onlyFrench).category()).isNull();
    }
}
