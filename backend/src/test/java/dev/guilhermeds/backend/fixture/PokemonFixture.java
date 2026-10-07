package dev.guilhermeds.backend.fixture;

import dev.guilhermeds.backend.domain.model.Ability;
import dev.guilhermeds.backend.domain.model.BaseStat;
import dev.guilhermeds.backend.domain.model.EvolutionStage;
import dev.guilhermeds.backend.domain.model.Height;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonDetail;
import dev.guilhermeds.backend.domain.model.PokemonProfile;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.StatName;
import dev.guilhermeds.backend.domain.model.Weight;

import java.util.Arrays;
import java.util.List;

// The canonical data, as the Pokémon repository returns it.
public final class PokemonFixture {

    private PokemonFixture() {
    }

    public static PokemonDetail pikachuDetail() {
        return new PokemonDetail(new PokedexNumber(25),
            new PokemonProfile("pikachu", "Mouse Pokémon", Height.fromDecimetres(4), Weight.fromHectograms(60),
                "https://img/25.png", "https://img/25-art.png", List.of(new PokemonType("electric")),
                List.of(new Ability("static", false)),
                Arrays.stream(StatName.values()).map(name -> new BaseStat(name, 50)).toList(),
                "It keeps its tail raised."),
            new EvolutionStage("pichu", new PokedexNumber(172), List.of(
                new EvolutionStage("pikachu", new PokedexNumber(25), List.of()))));
    }
}
