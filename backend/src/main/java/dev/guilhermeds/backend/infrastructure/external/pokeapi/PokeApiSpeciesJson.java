package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// GET /pokemon-species/{id}: only the fields we use.
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiSpeciesJson() {
}
