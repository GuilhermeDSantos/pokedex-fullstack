package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// GET /pokemon-species/{id}: only the fields we use.
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiSpeciesJson(List<Genus> genera,
                          @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries,
                          @JsonProperty("evolution_chain") NamedResource evolutionChain) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Genus(String genus, NamedResource language) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FlavorText(@JsonProperty("flavor_text") String text, NamedResource language, NamedResource version) {
    }
}
