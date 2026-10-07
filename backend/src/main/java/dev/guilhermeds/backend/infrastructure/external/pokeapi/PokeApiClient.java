package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.source.PokemonSourceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

// The HTTP calls to PokeAPI, one method per resource. Timeouts come from spring.http.clients.* (D-038).
@Component
public class PokeApiClient {

    private static final Logger log = LoggerFactory.getLogger(PokeApiClient.class);

    private final RestClient restClient;

    public PokeApiClient(RestClient.Builder builder, PokeApiProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl().toString()).build();
    }

    // Any offset is a valid page (past the end it's just empty), so a 404 means PokeAPI is broken.
    PokeApiPageJson fetchPage(long offset, int limit) {
        return get("/pokemon?offset={offset}&limit={limit}", PokeApiPageJson.class, offset, limit)
            .orElseThrow(() -> new PokemonSourceUnavailableException("PokeAPI returned incomplete data"));
    }

    Optional<PokeApiPokemonJson> fetchPokemon(String identifier) {
        return get("/pokemon/{identifier}", PokeApiPokemonJson.class, identifier);
    }

    // A 404 here means PokeAPI linked to a species it doesn't have: its data is broken, not our request.
    PokeApiSpeciesJson fetchSpecies(String url) {
        return get(url, PokeApiSpeciesJson.class)
            .orElseThrow(() -> new PokemonSourceUnavailableException("PokeAPI returned incomplete data"));
    }

    // 404 → empty; anything else that isn't a 2xx, a timeout or an I/O error → unavailable.
    private <T> Optional<T> get(String uri, Class<T> type, Object... variables) {
        try {
            return Optional.ofNullable(restClient.get().uri(uri, variables).retrieve().body(type));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("PokeAPI call failed: {}", uri);
            throw new PokemonSourceUnavailableException("PokeAPI is unavailable right now", e);
        }
    }
}
