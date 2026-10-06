package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.Weight;
import dev.guilhermeds.backend.domain.source.PokemonSummary;
import org.springframework.stereotype.Component;

import java.util.List;

// Pure PokeAPI JSON → domain translation; the HTTP side lives in the client.
@Component
public class PokeApiTranslator {

    PokemonSummary toSummary(PokeApiPokemonJson pokemon, PokeApiSpeciesJson species) {
        return new PokemonSummary(new PokedexNumber(pokemon.id()), pokemon.name(), null, null,
            Weight.fromHectograms(pokemon.weight()), List.of(), List.of());
    }
}
