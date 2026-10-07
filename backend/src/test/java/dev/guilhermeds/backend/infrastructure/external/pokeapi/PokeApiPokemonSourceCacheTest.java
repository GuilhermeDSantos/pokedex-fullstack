package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.source.PokemonSource;
import dev.guilhermeds.backend.infrastructure.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

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
}
