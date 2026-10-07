package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// GET /evolution-chain/{id}: a tree of species links.
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiEvolutionChainJson(Link chain) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Link(NamedResource species, @JsonProperty("evolves_to") List<Link> evolvesTo) {
    }
}
