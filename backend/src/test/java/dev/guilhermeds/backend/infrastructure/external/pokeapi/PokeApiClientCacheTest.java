package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.exception.PokemonDataUnavailableException;
import dev.guilhermeds.backend.infrastructure.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Each expectation allows one request, so a second HTTP call for the same resource fails the test.
@RestClientTest(PokeApiClient.class)
@Import({PokeApiConfig.class, CacheConfig.class})
@ImportAutoConfiguration(CacheAutoConfiguration.class)
class PokeApiClientCacheTest {

    private static final String BASE_URL = "https://pokeapi.co/api/v2";

    @Autowired
    private PokeApiClient client;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private CacheManager cacheManager;

    // The cache outlives a test (the context is shared), so each test starts from an empty one.
    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
    }

    @Test
    void shouldAskPokeApiOnlyOnceForTheSameResource() {
        var speciesUrl = BASE_URL + "/pokemon-species/25/";
        server.expect(requestTo(BASE_URL + "/pokemon?offset=0&limit=2"))
            .andRespond(withSuccess(json("pokemon-page-limit-2-offset-0.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/pokemon/25"))
            .andRespond(withSuccess(json("pokemon-25.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(speciesUrl))
            .andRespond(withSuccess(json("pokemon-species-25.json"), MediaType.APPLICATION_JSON));

        for (var round = 0; round < 2; round++) {
            client.fetchPage(0, 2);
            client.fetchPokemon("25");
            client.fetchSpecies(speciesUrl);
        }

        server.verify();
    }

    // An outage must not be remembered for six hours: the next call asks PokeAPI again.
    @Test
    void shouldNotCacheAFailure() {
        server.expect(once(), requestTo(BASE_URL + "/pokemon/25")).andRespond(withServerError());
        server.expect(once(), requestTo(BASE_URL + "/pokemon/25"))
            .andRespond(withSuccess(json("pokemon-25.json"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchPokemon("25")).isInstanceOf(PokemonDataUnavailableException.class);
        assertThat(client.fetchPokemon("25")).isPresent();
    }

    private static ClassPathResource json(String file) {
        return new ClassPathResource("pokeapi/" + file);
    }
}
