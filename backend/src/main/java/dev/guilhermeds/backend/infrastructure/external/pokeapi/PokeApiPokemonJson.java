package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// GET /pokemon/{id}: only the fields we use.
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiPokemonJson(int id, String name, int height, int weight, Sprites sprites, List<TypeSlot> types,
                          List<AbilitySlot> abilities, List<StatEntry> stats, NamedResource species) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Sprites(@JsonProperty("front_default") String frontDefault, Other other) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Other(@JsonProperty("official-artwork") Artwork officialArtwork) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Artwork(@JsonProperty("front_default") String frontDefault) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TypeSlot(int slot, NamedResource type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AbilitySlot(int slot, @JsonProperty("is_hidden") boolean hidden, NamedResource ability) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StatEntry(@JsonProperty("base_stat") int baseStat, NamedResource stat) {
    }
}
