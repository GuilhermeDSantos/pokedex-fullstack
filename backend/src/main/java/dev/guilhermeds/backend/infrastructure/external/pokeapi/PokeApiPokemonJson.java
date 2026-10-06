package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// GET /pokemon/{id}: only the fields we use.
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiPokemonJson(int id, String name, int weight) {
}
