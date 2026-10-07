package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("pokeapi")
public record PokeApiProperties(URI baseUrl) {
}
