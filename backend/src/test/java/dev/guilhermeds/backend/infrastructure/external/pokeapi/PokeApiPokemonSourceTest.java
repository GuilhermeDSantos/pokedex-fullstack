package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.source.PokemonSourceUnavailableException;
import dev.guilhermeds.backend.domain.source.PokemonSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.pokemon;
import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.species;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PokeApiPokemonSourceTest {

    @Mock
    private PokeApiClient client;

    private PokeApiPokemonSource source;

    @BeforeEach
    void setUp() {
        source = new PokeApiPokemonSource(client, new PokeApiTranslator());
    }

    @Test
    void shouldBuildAPageOfSummariesInPokeApiOrderWithTheTotal() {
        given(client.fetchPage(0, 2)).willReturn(page(1351, "bulbasaur", "pikachu"));
        givenPokemon("bulbasaur", 1);
        givenPokemon("pikachu", 25);

        var page = source.findAll(new PageRequest(0, 2));

        assertThat(page.totalElements()).isEqualTo(1351);
        assertThat(page.content()).extracting(PokemonSummary::number)
            .containsExactly(new PokedexNumber(1), new PokedexNumber(25));
    }

    // Bulbasaur's answer waits until Pikachu's request has started: one call at a time would never get there.
    @Test
    void shouldFetchTheEntriesConcurrentlyAndKeepPokeApiOrder() {
        var pikachuRequested = new CountDownLatch(1);
        given(client.fetchPage(0, 2)).willReturn(page(1351, "bulbasaur", "pikachu"));
        given(client.fetchPokemon("bulbasaur")).willAnswer(invocation -> {
            if (!pikachuRequested.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("the entries were fetched one at a time");
            }
            return Optional.of(pokemon(1));
        });
        given(client.fetchPokemon("pikachu")).willAnswer(invocation -> {
            pikachuRequested.countDown();
            return Optional.of(pokemon(25));
        });
        given(client.fetchSpecies(pokemon(1).species().url())).willReturn(species(1));
        given(client.fetchSpecies(pokemon(25).species().url())).willReturn(species(25));

        var page = source.findAll(new PageRequest(0, 2));

        assertThat(page.content()).extracting(PokemonSummary::number)
            .containsExactly(new PokedexNumber(1), new PokedexNumber(25));
    }

    // A card can't silently vanish: PokeAPI contradicting its own list is an outage.
    @Test
    void shouldReportPokeApiAsUnavailableWhenItCannotReturnAPokemonItListed() {
        given(client.fetchPage(0, 1)).willReturn(page(1351, "missingno"));
        given(client.fetchPokemon("missingno")).willReturn(Optional.empty());

        assertThatThrownBy(() -> source.findAll(new PageRequest(0, 1)))
            .isInstanceOf(PokemonSourceUnavailableException.class);
    }

    private void givenPokemon(String name, int id) {
        given(client.fetchPokemon(name)).willReturn(Optional.of(pokemon(id)));
        given(client.fetchSpecies(pokemon(id).species().url())).willReturn(species(id));
    }

    private static PokeApiPageJson page(int count, String... names) {
        var results = List.of(names).stream()
            .map(name -> new NamedResource(name, "https://pokeapi.co/api/v2/pokemon/" + name + "/"))
            .toList();
        return new PokeApiPageJson(count, results);
    }
}
