package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.source.PokemonSourceUnavailableException;
import dev.guilhermeds.backend.domain.source.PokemonSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.evolutionChain;
import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.pokemon;
import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.species;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PokeApiPokemonSourceTest {

    @Mock
    private PokeApiClient client;

    private PokeApiPokemonSource source;

    @BeforeEach
    void setUp() {
        source = new PokeApiPokemonSource(client, new PokeApiTranslator(), properties(10));
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

    // PokeAPI is a free public service: a page of 50 must not fire 100 calls at once (D-018).
    @Test
    void shouldNeverRunMoreCallsAtOnceThanTheConfiguredMaximum() {
        var limited = new PokeApiPokemonSource(client, new PokeApiTranslator(), properties(2));
        var inFlight = new AtomicInteger();
        var peak = new AtomicInteger();
        given(client.fetchPage(0, 4)).willReturn(page(1351, "a", "b", "c", "d"));
        given(client.fetchPokemon(anyString())).willAnswer(invocation -> {
            peak.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            Thread.sleep(200);
            inFlight.decrementAndGet();
            return Optional.of(pokemon(25));
        });
        given(client.fetchSpecies(pokemon(25).species().url())).willReturn(species(25));

        limited.findAll(new PageRequest(0, 4));

        assertThat(peak).hasValue(2);
    }

    // A card can't silently vanish: PokeAPI contradicting its own list is an outage.
    @Test
    void shouldReportPokeApiAsUnavailableWhenItCannotReturnAPokemonItListed() {
        given(client.fetchPage(0, 1)).willReturn(page(1351, "missingno"));
        given(client.fetchPokemon("missingno")).willReturn(Optional.empty());

        assertThatThrownBy(() -> source.findAll(new PageRequest(0, 1)))
            .isInstanceOf(PokemonSourceUnavailableException.class);
    }

    // The failure a worker thread hit reaches the caller as itself, so it still maps to a 503.
    @Test
    void shouldPassOnAFailureFromAConcurrentCallUnwrapped() {
        given(client.fetchPage(0, 1)).willReturn(page(1351, "pikachu"));
        given(client.fetchPokemon("pikachu")).willReturn(Optional.of(pokemon(25)));
        given(client.fetchSpecies(pokemon(25).species().url()))
            .willThrow(new PokemonSourceUnavailableException("PokeAPI is unavailable right now"));

        assertThatThrownBy(() -> source.findAll(new PageRequest(0, 1)))
            .isExactlyInstanceOf(PokemonSourceUnavailableException.class)
            .hasMessage("PokeAPI is unavailable right now");
    }

    // ---- one Pokémon (US-02) ------------------------------------------------------------------

    // Each call follows the URL the previous answer gave: Pokémon → species → evolution chain.
    @Test
    void shouldBuildTheDetailFromThePokemonItsSpeciesAndItsChain() {
        given(client.fetchPokemon("pikachu")).willReturn(Optional.of(pokemon(25)));
        given(client.fetchSpecies(pokemon(25).species().url())).willReturn(species(25));
        given(client.fetchEvolutionChain(species(25).evolutionChain().url())).willReturn(evolutionChain(10));

        var detail = source.findByIdentifier(new PokemonIdentifier("Pikachu"));

        assertThat(detail).hasValueSatisfying(pikachu -> {
            assertThat(pikachu.number()).isEqualTo(new PokedexNumber(25));
            assertThat(pikachu.evolutionChain().speciesName()).isEqualTo("pichu");
        });
    }

    @Test
    void shouldFindNothingWhenPokeApiDoesNotKnowThePokemon() {
        given(client.fetchPokemon("missingno")).willReturn(Optional.empty());

        assertThat(source.findByIdentifier(new PokemonIdentifier("missingno"))).isEmpty();
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

    private static PokeApiProperties properties(int maxConcurrency) {
        return new PokeApiProperties(URI.create("https://pokeapi.co/api/v2"), maxConcurrency);
    }
}
