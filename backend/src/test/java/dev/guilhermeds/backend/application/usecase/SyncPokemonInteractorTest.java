package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.SyncPokemonInput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.domain.exception.PokemonAlreadySyncedException;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;
import dev.guilhermeds.backend.fixture.LocalPokemonFixture;
import dev.guilhermeds.backend.fixture.PokemonFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Supplier;

import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.NOW;
import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.PIKACHU_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SyncPokemonInteractorTest {

    @Mock private PokemonRepository pokemonRepository;
    @Mock private LocalPokemonRepository localPokemonRepository;
    @Mock private UnitOfWork unitOfWork;

    private SyncPokemonInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new SyncPokemonInteractor(pokemonRepository, localPokemonRepository, new PokemonMapper(), unitOfWork);
        lenient().when(unitOfWork.inTransaction(ArgumentMatchers.<Supplier<Object>>any()))
            .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
    }

    @Test
    void shouldRecordThePokemonUnderItsNumberWithTheGivenIdAndTime() {
        given(pokemonRepository.getByIdentifier(new PokemonIdentifier("pikachu"))).willReturn(PokemonFixture.pikachuDetail());
        given(localPokemonRepository.save(any(LocalPokemon.class))).willAnswer(invocation -> invocation.getArgument(0));

        var output = interactor.execute(new SyncPokemonInput("Pikachu"), PIKACHU_ID, NOW);

        assertThat(output.pokedexNumber()).isEqualTo(25);
        assertThat(output.tags()).isEmpty();
        assertThat(output.syncedAt()).isEqualTo(NOW);
        then(localPokemonRepository).should().save(argThat(saved -> saved.getId().equals(PIKACHU_ID)));
    }

    // The unique number in the database still decides when two syncs race; this gives the usual case its 409.
    @Test
    void shouldRefuseAPokemonThatIsAlreadySynced() {
        given(pokemonRepository.getByIdentifier(new PokemonIdentifier("pikachu"))).willReturn(PokemonFixture.pikachuDetail());
        given(localPokemonRepository.findByPokedexNumber(new PokedexNumber(25)))
            .willReturn(Optional.of(LocalPokemonFixture.syncedPikachu()));

        assertThatThrownBy(() -> interactor.execute(new SyncPokemonInput("pikachu"), PIKACHU_ID, NOW))
            .isInstanceOf(PokemonAlreadySyncedException.class);

        then(localPokemonRepository).should(never()).save(any());
    }
}
