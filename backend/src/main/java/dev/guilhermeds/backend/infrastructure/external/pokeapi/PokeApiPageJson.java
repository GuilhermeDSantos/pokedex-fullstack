package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// GET /pokemon?offset=&limit=: the total and one name/url per entry.
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiPageJson(int count, List<NamedResource> results) {
}
