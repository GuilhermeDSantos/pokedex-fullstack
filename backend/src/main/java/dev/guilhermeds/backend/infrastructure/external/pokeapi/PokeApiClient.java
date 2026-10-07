package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

// The HTTP calls to PokeAPI, one method per resource. Timeouts come from spring.http.clients.* (D-038).
@Component
public class PokeApiClient {

    private final RestClient restClient;

    public PokeApiClient(RestClient.Builder builder, PokeApiProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl().toString()).build();
    }

    Optional<PokeApiPokemonJson> fetchPokemon(String identifier) {
        try {
            return Optional.ofNullable(restClient.get().uri("/pokemon/{identifier}", identifier).retrieve()
                .body(PokeApiPokemonJson.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
