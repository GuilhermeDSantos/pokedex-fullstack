package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("pokeapi")
public record PokeApiProperties(URI baseUrl, int maxConcurrency) {

    public PokeApiProperties {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("pokeapi.max-concurrency must be at least 1");
        }
    }
}
