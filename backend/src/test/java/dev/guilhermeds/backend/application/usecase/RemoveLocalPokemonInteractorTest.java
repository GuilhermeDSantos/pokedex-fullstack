package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.RemoveLocalPokemonInput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.fixture.LocalPokemonFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RemoveLocalPokemonInteractorTest {

    @Mock private LocalPokemonRepository localPokemonRepository;
    @Mock private UnitOfWork unitOfWork;

    private RemoveLocalPokemonInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new RemoveLocalPokemonInteractor(localPokemonRepository, new PokemonMapper(), unitOfWork);
        // Run the Runnable form inline: removing returns nothing.
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(unitOfWork).inTransaction(any(Runnable.class));
    }

    @Test
    void shouldDeleteTheRecordOfTheGivenPokemon() {
        var pikachu = LocalPokemonFixture.syncedPikachu();
        given(localPokemonRepository.getByPokedexNumber(new PokedexNumber(25))).willReturn(pikachu);

        interactor.execute(new RemoveLocalPokemonInput("25"));

        then(localPokemonRepository).should().delete(pikachu);
    }
}
