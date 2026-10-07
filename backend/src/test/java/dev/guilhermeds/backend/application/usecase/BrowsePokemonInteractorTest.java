package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.AbilityOutput;
import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.domain.model.Ability;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.Weight;
import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.source.PokemonSource;
import dev.guilhermeds.backend.domain.source.PokemonSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BrowsePokemonInteractorTest {

    private static final PokemonSummary PIKACHU = new PokemonSummary(new PokedexNumber(25), "pikachu",
        "https://img/25.png", "Mouse Pokémon", Weight.fromHectograms(60), List.of(new PokemonType("electric")),
        List.of(new Ability("static", false), new Ability("lightning-rod", true)));

    @Mock
    private PokemonSource source;

    private BrowsePokemonInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new BrowsePokemonInteractor(source);
    }

    @Test
    void shouldReturnTheRequestedPageOfCards() {
        given(source.findAll(new PageRequest(1, 20))).willReturn(new Page<>(List.of(PIKACHU), 1351));

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
}
