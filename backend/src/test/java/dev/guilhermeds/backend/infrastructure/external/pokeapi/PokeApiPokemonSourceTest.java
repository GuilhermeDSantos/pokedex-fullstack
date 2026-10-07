package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.source.PokemonSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.pokemon;
import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.species;
import static org.assertj.core.api.Assertions.assertThat;
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
