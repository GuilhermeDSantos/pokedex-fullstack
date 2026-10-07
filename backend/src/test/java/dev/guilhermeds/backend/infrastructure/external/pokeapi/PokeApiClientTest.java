package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// The HTTP side against a mock server, with recorded PokeAPI JSON: never the network.
@RestClientTest(PokeApiClient.class)
@Import(PokeApiConfig.class)
class PokeApiClientTest {

    private static final String BASE_URL = "https://pokeapi.co/api/v2";

    @Autowired
    private PokeApiClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void shouldFetchAPokemonByNameOrNumber() {
        server.expect(requestTo(BASE_URL + "/pokemon/25"))
            .andRespond(withSuccess(new ClassPathResource("pokeapi/pokemon-25.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchPokemon("25")).hasValueSatisfying(pokemon -> assertThat(pokemon.name()).isEqualTo("pikachu"));
    }
}
