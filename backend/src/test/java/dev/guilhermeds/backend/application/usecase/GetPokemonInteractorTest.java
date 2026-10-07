package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.EvolutionStageOutput;
import dev.guilhermeds.backend.application.dto.GetPokemonInput;
import dev.guilhermeds.backend.application.dto.StatOutput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.domain.exception.InvalidPokemonIdentifierException;
import dev.guilhermeds.backend.domain.model.Ability;
import dev.guilhermeds.backend.domain.model.BaseStat;
import dev.guilhermeds.backend.domain.model.Height;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.model.PokemonProfile;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.StatName;
import dev.guilhermeds.backend.domain.model.Weight;
import dev.guilhermeds.backend.domain.source.EvolutionStage;
import dev.guilhermeds.backend.domain.source.PokemonDetail;
import dev.guilhermeds.backend.domain.source.PokemonSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetPokemonInteractorTest {

    private static final PokemonDetail PIKACHU = new PokemonDetail(new PokedexNumber(25),
        new PokemonProfile("pikachu", "Mouse Pokémon", Height.fromDecimetres(4), Weight.fromHectograms(60),
            "https://img/25.png", "https://img/25-art.png", List.of(new PokemonType("electric")),
            List.of(new Ability("static", false)),
            Arrays.stream(StatName.values()).map(name -> new BaseStat(name, 50)).toList(),
            "It keeps its tail raised."),
        new EvolutionStage("pichu", new PokedexNumber(172), List.of(
            new EvolutionStage("pikachu", new PokedexNumber(25), List.of()))));

    @Mock
    private PokemonSource source;

    private GetPokemonInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new GetPokemonInteractor(source, new PokemonMapper());
    }

    @Test
    void shouldDescribeThePokemonTheIdentifierNames() {
        // getByIdentifier is a default method: Mockito doesn't run default bodies, so it's stubbed directly.
        given(source.getByIdentifier(new PokemonIdentifier("pikachu"))).willReturn(PIKACHU);

        var output = interactor.execute(new GetPokemonInput("Pikachu"));

        assertThat(output.pokedexNumber()).isEqualTo(25);
        assertThat(output.name()).isEqualTo("pikachu");
        assertThat(output.heightMeters()).isEqualTo(new BigDecimal("0.4"));
        assertThat(output.weightKilograms()).isEqualTo(new BigDecimal("6.0"));
        assertThat(output.artworkUrl()).isEqualTo("https://img/25-art.png");
        assertThat(output.description()).isEqualTo("It keeps its tail raised.");
        assertThat(output.stats()).first().isEqualTo(new StatOutput("HP", 50));
        assertThat(output.evolutionChain()).isEqualTo(new EvolutionStageOutput("pichu", 172,
            List.of(new EvolutionStageOutput("pikachu", 25, List.of()))));
    }

    @Test
    void shouldRejectAMalformedIdentifierBeforeCallingPokeApi() {
        assertThatThrownBy(() -> interactor.execute(new GetPokemonInput("pika chu")))
            .isInstanceOf(InvalidPokemonIdentifierException.class);

        verifyNoInteractions(source);
    }
}
