package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.Weight;
import dev.guilhermeds.backend.domain.source.PokemonSummary;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

// Pure PokeAPI JSON → domain translation; the HTTP side lives in the client.
@Component
public class PokeApiTranslator {

    private static final String ENGLISH = "en";

    PokemonSummary toSummary(PokeApiPokemonJson pokemon, PokeApiSpeciesJson species) {
        return new PokemonSummary(new PokedexNumber(pokemon.id()), pokemon.name(), pokemon.sprites().frontDefault(), englishGenus(species),
            Weight.fromHectograms(pokemon.weight()), types(pokemon), List.of());
    }

    private static List<PokemonType> types(PokeApiPokemonJson pokemon) {
        return pokemon.types().stream()
            .sorted(Comparator.comparingInt(PokeApiPokemonJson.TypeSlot::slot))
            .map(slot -> new PokemonType(slot.type().name()))
            .toList();
    }

    private static String englishGenus(PokeApiSpeciesJson species) {
        return species.genera().stream()
            .filter(genus -> ENGLISH.equals(genus.language().name()))
            .map(PokeApiSpeciesJson.Genus::genus)
            .findFirst()
            .orElse(null);
    }
}
