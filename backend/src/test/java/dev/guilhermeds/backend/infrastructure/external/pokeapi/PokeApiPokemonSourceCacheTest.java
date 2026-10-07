package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.source.PokemonSource;
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

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Through the port, the way the use cases call it: the source reaches the cache only because the
 * client is a separate bean. Merged into one class, these would be self-calls that skip the proxy.
 */
@RestClientTest(components = {PokeApiClient.class, PokeApiPokemonSource.class, PokeApiTranslator.class})
@Import({PokeApiConfig.class, CacheConfig.class})
@ImportAutoConfiguration(CacheAutoConfiguration.class)
class PokeApiPokemonSourceCacheTest {

    private static final String BASE_URL = "https://pokeapi.co/api/v2";

    @Autowired
    private PokemonSource source;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private CacheManager cacheManager;

    // Both tests ask for Pikachu, and the cache outlives a test in the shared context.
    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(Objects::nonNull)
            .forEach(Cache::clear);
    }

    @Test
    void shouldServeARepeatedPageFromTheCache() {
        server.expect(requestTo(BASE_URL + "/pokemon?offset=0&limit=1")).andRespond(withSuccess("""
            { "count": 1351, "results": [ { "name": "pikachu", "url": "https://pokeapi.co/api/v2/pokemon/25/" } ] }
            """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/pokemon/pikachu"))
            .andRespond(withSuccess(new ClassPathResource("pokeapi/pokemon-25.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/pokemon-species/25/"))
            .andRespond(withSuccess(new ClassPathResource("pokeapi/pokemon-species-25.json"), MediaType.APPLICATION_JSON));

        source.findAll(new PageRequest(0, 1));
        source.findAll(new PageRequest(0, 1));

        server.verify();
    }

    // getByIdentifier is the port's default method: it calls findByIdentifier on `this`. Caching on
    // the client is what keeps that self-call cached (D-012).
    @Test
    void shouldServeARepeatedDetailFromTheCacheThroughTheDefaultMethod() {
        server.expect(requestTo(BASE_URL + "/pokemon/pikachu"))
            .andRespond(withSuccess(new ClassPathResource("pokeapi/pokemon-25.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/pokemon-species/25/"))
            .andRespond(withSuccess(new ClassPathResource("pokeapi/pokemon-species-25.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/evolution-chain/10/"))
            .andRespond(withSuccess(new ClassPathResource("pokeapi/evolution-chain-10.json"), MediaType.APPLICATION_JSON));

        source.getByIdentifier(new PokemonIdentifier("pikachu"));
        source.getByIdentifier(new PokemonIdentifier("pikachu"));

        server.verify();
    }
}
