package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PokeApiProperties.class)
public class PokeApiConfig {
}
