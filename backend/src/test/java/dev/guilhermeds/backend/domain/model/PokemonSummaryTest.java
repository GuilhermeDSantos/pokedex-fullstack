package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.model.Ability;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.Weight;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonSummaryTest {

    private static final PokedexNumber NUMBER = new PokedexNumber(25);
    private static final Weight WEIGHT = Weight.fromHectograms(60);
    private static final List<PokemonType> TYPES = List.of(new PokemonType("electric"));
    private static final List<Ability> ABILITIES = List.of(new Ability("static", false));

    @Test
    void shouldRequireTheNumberNameAndWeight() {
        assertThatThrownBy(() -> new PokemonSummary(null, "pikachu", "sprite.png", "Mouse Pokémon", WEIGHT, TYPES, ABILITIES))
            .hasMessage("number must not be null");
        assertThatThrownBy(() -> new PokemonSummary(NUMBER, null, "sprite.png", "Mouse Pokémon", WEIGHT, TYPES, ABILITIES))
            .hasMessage("name must not be null");
        assertThatThrownBy(() -> new PokemonSummary(NUMBER, "pikachu", "sprite.png", "Mouse Pokémon", null, TYPES, ABILITIES))
            .hasMessage("weight must not be null");
    }

    @Test
    void shouldKeepItsOwnCopyOfTheTypesAndAbilities() {
        var types = new ArrayList<>(TYPES);
        var abilities = new ArrayList<>(ABILITIES);
        var summary = new PokemonSummary(NUMBER, "pikachu", "sprite.png", "Mouse Pokémon", WEIGHT, types, abilities);

        types.add(new PokemonType("fairy"));
        abilities.clear();

        assertThat(summary.types()).containsExactly(new PokemonType("electric"));
        assertThat(summary.abilities()).containsExactly(new Ability("static", false));
        assertThatThrownBy(() -> summary.types().add(new PokemonType("fairy")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // PokeAPI has gaps: some forms have no sprite, and a few species no English genus.
    @Test
    void shouldAcceptAMissingSpriteAndCategory() {
        var summary = new PokemonSummary(NUMBER, "pikachu", null, null, WEIGHT, TYPES, ABILITIES);

        assertThat(summary.spriteUrl()).isNull();
        assertThat(summary.category()).isNull();
    }
}
