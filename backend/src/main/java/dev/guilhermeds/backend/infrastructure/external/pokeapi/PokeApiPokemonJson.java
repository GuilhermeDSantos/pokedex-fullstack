package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// GET /pokemon/{id}: only the fields we use.
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiPokemonJson(int id, String name, int weight, Sprites sprites) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Sprites(@JsonProperty("front_default") String frontDefault) {
    }
}
