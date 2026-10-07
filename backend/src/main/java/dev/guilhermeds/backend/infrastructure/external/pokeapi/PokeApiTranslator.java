package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.Ability;
import dev.guilhermeds.backend.domain.model.BaseStat;
import dev.guilhermeds.backend.domain.model.Height;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonProfile;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.StatName;
import dev.guilhermeds.backend.domain.model.Weight;
import dev.guilhermeds.backend.domain.source.EvolutionStage;
import dev.guilhermeds.backend.domain.source.PokemonDetail;
import dev.guilhermeds.backend.domain.source.PokemonSummary;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Pure PokeAPI JSON → domain translation; the HTTP side lives in the client.
@Component
public class PokeApiTranslator {

    private static final String ENGLISH = "en";

    PokemonSummary toSummary(PokeApiPokemonJson pokemon, PokeApiSpeciesJson species) {
        return new PokemonSummary(
            new PokedexNumber(pokemon.id()),
            pokemon.name(),
            pokemon.sprites().frontDefault(),
            englishGenus(species),
            Weight.fromHectograms(pokemon.weight()),
            types(pokemon),
            abilities(pokemon));
    }

    PokemonDetail toDetail(PokeApiPokemonJson pokemon, PokeApiSpeciesJson species, PokeApiEvolutionChainJson chain) {
        var profile = new PokemonProfile(
            pokemon.name(),
            englishGenus(species),
            Height.fromDecimetres(pokemon.height()),
            Weight.fromHectograms(pokemon.weight()),
            pokemon.sprites().frontDefault(),
            pokemon.sprites().other().officialArtwork().frontDefault(),
            types(pokemon),
            abilities(pokemon),
            stats(pokemon),
            englishDescription(species));
        return new PokemonDetail(new PokedexNumber(pokemon.id()), profile, stage(chain.chain()));
    }

    // PokeAPI names stats "special-attack"; the domain calls it SPECIAL_ATTACK.
    private static List<BaseStat> stats(PokeApiPokemonJson pokemon) {
        return pokemon.stats().stream()
            .map(entry -> new BaseStat(
                StatName.valueOf(entry.stat().name().toUpperCase(Locale.ROOT).replace('-', '_')), entry.baseStat()))
            .toList();
    }

    // Each game version has its own text; the newest English one is the current wording.
    private static String englishDescription(PokeApiSpeciesJson species) {
        return species.flavorTextEntries().stream()
            .filter(entry -> ENGLISH.equals(entry.language().name()))
            .max(Comparator.comparingInt(entry -> idFromUrl(entry.version().url())))
            .map(entry -> normalize(entry.text()))
            .orElse(null);
    }

    // The games' text keeps their line breaks and form feeds; soft hyphens only mark where a word may break.
    private static String normalize(String text) {
        return text.replace("\u00AD", "").replaceAll("\\s+", " ").trim();
    }

    private static EvolutionStage stage(PokeApiEvolutionChainJson.Link link) {
        return new EvolutionStage(link.species().name(), numberFromUrl(link.species().url()),
            link.evolvesTo().stream().map(PokeApiTranslator::stage).toList());
    }

    // A chain names each species only by its URL (".../pokemon-species/172/"); the id is the Pokédex number.
    private static PokedexNumber numberFromUrl(String url) {
        return new PokedexNumber(idFromUrl(url));
    }

    private static int idFromUrl(String url) {
        var path = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
    }

    private static List<PokemonType> types(PokeApiPokemonJson pokemon) {
        return pokemon.types().stream()
            .sorted(Comparator.comparingInt(PokeApiPokemonJson.TypeSlot::slot))
            .map(slot -> new PokemonType(slot.type().name()))
            .toList();
    }

    private static List<Ability> abilities(PokeApiPokemonJson pokemon) {
        return pokemon.abilities().stream()
            .sorted(Comparator.comparingInt(PokeApiPokemonJson.AbilitySlot::slot))
            .map(slot -> new Ability(slot.ability().name(), slot.hidden()))
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
