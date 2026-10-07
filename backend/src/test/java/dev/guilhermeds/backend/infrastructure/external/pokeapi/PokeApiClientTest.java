package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.exception.PokemonDataUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
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

    // An unknown name or number is a fact about the catalog, not an outage.
    @Test
    void shouldReturnNothingWhenPokeApiDoesNotKnowThePokemon() {
        server.expect(requestTo(BASE_URL + "/pokemon/missingno")).andRespond(withResourceNotFound());

        assertThat(client.fetchPokemon("missingno")).isEmpty();
    }

    @Test
    void shouldReportPokeApiAsUnavailableWhenItFails() {
        server.expect(requestTo(BASE_URL + "/pokemon/25")).andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchPokemon("25"))
            .isInstanceOf(PokemonDataUnavailableException.class)
            .hasMessage("PokeAPI is unavailable right now");
    }

    @Test
    void shouldReportPokeApiAsUnavailableWhenItCannotBeReached() {
        server.expect(requestTo(BASE_URL + "/pokemon/25")).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.fetchPokemon("25")).isInstanceOf(PokemonDataUnavailableException.class);
    }

    // Followed by the URL the Pokémon response gave: alternate forms have a different species id.
    @Test
    void shouldFetchTheSpeciesFromTheUrlPokeApiGave() {
        var url = BASE_URL + "/pokemon-species/25/";
        server.expect(requestTo(url))
            .andRespond(withSuccess(new ClassPathResource("pokeapi/pokemon-species-25.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchSpecies(url).genera()).isNotEmpty();
    }

    @Test
    void shouldReportPokeApiAsUnavailableWhenTheSpeciesItLinkedIsMissing() {
        var url = BASE_URL + "/pokemon-species/25/";
        server.expect(requestTo(url)).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.fetchSpecies(url))
            .isInstanceOf(PokemonDataUnavailableException.class)
            .hasMessage("PokeAPI returned incomplete data");
    }

    @Test
    void shouldFetchAPageOfTheList() {
        server.expect(requestTo(BASE_URL + "/pokemon?offset=0&limit=2"))
            .andRespond(withSuccess(new ClassPathResource("pokeapi/pokemon-page-limit-2-offset-0.json"), MediaType.APPLICATION_JSON));

        var page = client.fetchPage(0, 2);

        assertThat(page.count()).isEqualTo(1351);
        assertThat(page.results()).extracting(NamedResource::name).containsExactly("bulbasaur", "ivysaur");
    }

    @Test
    void shouldFetchTheEvolutionChainFromTheUrlTheSpeciesGave() {
        var url = BASE_URL + "/evolution-chain/10/";
        server.expect(requestTo(url))
            .andRespond(withSuccess(new ClassPathResource("pokeapi/evolution-chain-10.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchEvolutionChain(url).chain().species().name()).isEqualTo("pichu");
    }
}
