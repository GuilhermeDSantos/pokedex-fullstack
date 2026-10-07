package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetLocalPokemonInput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.domain.exception.InvalidPokedexNumberException;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;
import dev.guilhermeds.backend.fixture.LocalPokemonFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetLocalPokemonInteractorTest {

    @Mock private PokemonRepository pokemonRepository;
    @Mock private LocalPokemonRepository localPokemonRepository;

    private GetLocalPokemonInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new GetLocalPokemonInteractor(pokemonRepository, localPokemonRepository, new PokemonMapper());
    }

    // A number is the record's own key, so the canonical data isn't needed at all.
    @Test
    void shouldFindTheRecordByNumberWithoutTheCanonicalData() {
        given(localPokemonRepository.getByPokedexNumber(new PokedexNumber(25))).willReturn(LocalPokemonFixture.renamedPikachu());

        var output = interactor.execute(new GetLocalPokemonInput("25"));

        assertThat(output.pokedexNumber()).isEqualTo(25);
        assertThat(output.localizedName()).isEqualTo("Pica");
        assertThat(output.tags()).containsExactly("mascot", "starter");
        verifyNoInteractions(pokemonRepository);
    }

    // Our records are addressed by number only (D-040): a name is a 400, with no lookup anywhere.
    @Test
    void shouldRejectANameWithoutLookingAnythingUp() {
        assertThatThrownBy(() -> interactor.execute(new GetLocalPokemonInput("pikachu")))
            .isInstanceOf(InvalidPokedexNumberException.class);

        verifyNoInteractions(pokemonRepository, localPokemonRepository);
    }
}
