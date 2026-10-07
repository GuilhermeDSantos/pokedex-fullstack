package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.AbilityOutput;
import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.domain.exception.InvalidPageRequestException;
import dev.guilhermeds.backend.domain.model.Ability;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.Weight;
import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;
import dev.guilhermeds.backend.fixture.LocalPokemonFixture;
import dev.guilhermeds.backend.domain.model.PokemonSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BrowsePokemonInteractorTest {

    private static final PokemonSummary PIKACHU = new PokemonSummary(new PokedexNumber(25), "pikachu",
        "https://img/25.png", "Mouse Pokémon", Weight.fromHectograms(60), List.of(new PokemonType("electric")),
        List.of(new Ability("static", false), new Ability("lightning-rod", true)));

    private static final PokemonSummary BULBASAUR = new PokemonSummary(new PokedexNumber(1), "bulbasaur",
        "https://img/1.png", "Seed Pokémon", Weight.fromHectograms(69), List.of(new PokemonType("grass")),
        List.of(new Ability("overgrow", false)));

    @Mock
    private PokemonRepository pokemonRepository;

    @Mock
    private LocalPokemonRepository localPokemonRepository;

    private BrowsePokemonInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new BrowsePokemonInteractor(pokemonRepository, localPokemonRepository);
    }

    @Test
    void shouldReturnTheRequestedPageOfCards() {
        given(pokemonRepository.findAll(new PageRequest(1, 20))).willReturn(new Page<>(List.of(PIKACHU), 1351));

        var output = interactor.execute(new BrowsePokemonInput(1, 20));

        assertThat(output.page()).isEqualTo(1);
        assertThat(output.size()).isEqualTo(20);
        assertThat(output.totalElements()).isEqualTo(1351);
        assertThat(output.content()).singleElement().satisfies(card -> {
            assertThat(card.pokedexNumber()).isEqualTo(25);
            assertThat(card.name()).isEqualTo("pikachu");
            assertThat(card.spriteUrl()).isEqualTo("https://img/25.png");
            assertThat(card.category()).isEqualTo("Mouse Pokémon");
            assertThat(card.weightKilograms()).isEqualTo(new BigDecimal("6.0"));
            assertThat(card.types()).containsExactly("electric");
            assertThat(card.abilities())
                .containsExactly(new AbilityOutput("static", false), new AbilityOutput("lightning-rod", true));
        });
    }

    // Our records for the whole page come in one lookup; only a synced Pokémon has a localized name.
    @Test
    void shouldGiveEachCardItsLocalizedNameFromOneLookupForThePage() {
        given(pokemonRepository.findAll(new PageRequest(0, 20))).willReturn(new Page<>(List.of(BULBASAUR, PIKACHU), 1351));
        given(localPokemonRepository.findAllByPokedexNumbers(List.of(new PokedexNumber(1), new PokedexNumber(25))))
            .willReturn(List.of(LocalPokemonFixture.renamedPikachu()));

        var output = interactor.execute(new BrowsePokemonInput(0, 20));

        assertThat(output.content()).extracting(card -> card.localizedName()).containsExactly(null, "Pica");
        then(localPokemonRepository).should().findAllByPokedexNumbers(List.of(new PokedexNumber(1), new PokedexNumber(25)));
    }

    // Each card costs two PokeAPI calls, so a bad page request must fail before any of them.
    @Test
    void shouldRejectAnOversizedPageBeforeCallingPokeApi() {
        assertThatThrownBy(() -> interactor.execute(new BrowsePokemonInput(0, 51)))
            .isInstanceOf(InvalidPageRequestException.class);

        verifyNoInteractions(pokemonRepository, localPokemonRepository);
    }
}
