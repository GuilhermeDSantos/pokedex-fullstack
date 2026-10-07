package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// GET /pokemon/{id}: only the fields we use.
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiPokemonJson(int id, String name, int weight, Sprites sprites, List<TypeSlot> types,
                          List<AbilitySlot> abilities, NamedResource species) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Sprites(@JsonProperty("front_default") String frontDefault) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TypeSlot(int slot, NamedResource type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AbilitySlot(int slot, @JsonProperty("is_hidden") boolean hidden, NamedResource ability) {
    }
}
