package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// GET /pokemon-species/{id}: only the fields we use.
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiSpeciesJson(List<Genus> genera) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Genus(String genus, NamedResource language) {
    }
}
