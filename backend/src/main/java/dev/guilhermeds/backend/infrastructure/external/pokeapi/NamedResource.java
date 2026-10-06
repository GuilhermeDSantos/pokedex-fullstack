package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// PokeAPI's link to another resource: { "name": ..., "url": ... }.
@JsonIgnoreProperties(ignoreUnknown = true)
record NamedResource(String name, String url) {
}
