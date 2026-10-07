package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.UpdateLocalPokemonInput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.fixture.LocalPokemonFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UpdateLocalPokemonInteractorTest {

    private static final Instant LATER = NOW.plusSeconds(3600);

    @Mock private LocalPokemonRepository localPokemonRepository;
    @Mock private UnitOfWork unitOfWork;

    private UpdateLocalPokemonInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new UpdateLocalPokemonInteractor(localPokemonRepository, new PokemonMapper(), unitOfWork);
        lenient().when(unitOfWork.inTransaction(ArgumentMatchers.<Supplier<Object>>any()))
            .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
    }

    @Test
    void shouldReplaceOurFieldsAndSaveTheRecord() {
        given(localPokemonRepository.getByPokedexNumber(new PokedexNumber(25))).willReturn(LocalPokemonFixture.renamedPikachu());
        given(localPokemonRepository.save(any(LocalPokemon.class))).willAnswer(invocation -> invocation.getArgument(0));

        var output = interactor.execute(
            new UpdateLocalPokemonInput("25", "Pikachu BR", "Johto", List.of("Starter", "electric")), LATER);

        assertThat(output.localizedName()).isEqualTo("Pikachu BR");
        assertThat(output.region()).isEqualTo("Johto");
        assertThat(output.tags()).containsExactly("electric", "starter");
        assertThat(output.updatedAt()).isEqualTo(LATER);
    }
}
