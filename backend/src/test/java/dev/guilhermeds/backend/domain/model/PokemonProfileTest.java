package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonProfileTest {

    private static final Height HEIGHT = Height.fromDecimetres(4);
    private static final Weight WEIGHT = Weight.fromHectograms(60);
    private static final List<PokemonType> TYPES = List.of(new PokemonType("electric"));
    private static final List<Ability> ABILITIES = List.of(new Ability("static", false));
    private static final List<BaseStat> STATS = Arrays.stream(StatName.values()).map(name -> new BaseStat(name, 50)).toList();

    @Test
    void shouldRequireTheNameHeightAndWeight() {
        assertThatThrownBy(() -> profile(null, HEIGHT, WEIGHT, STATS)).hasMessage("name must not be null");
        assertThatThrownBy(() -> profile("pikachu", null, WEIGHT, STATS)).hasMessage("height must not be null");
        assertThatThrownBy(() -> profile("pikachu", HEIGHT, null, STATS)).hasMessage("weight must not be null");
    }

    private static PokemonProfile profile(String name, Height height, Weight weight, List<BaseStat> stats) {
        return new PokemonProfile(name, "Mouse Pokémon", height, weight, "sprite.png", "artwork.png", TYPES, ABILITIES,
            stats, "It keeps its tail raised to monitor its surroundings.");
    }
}
