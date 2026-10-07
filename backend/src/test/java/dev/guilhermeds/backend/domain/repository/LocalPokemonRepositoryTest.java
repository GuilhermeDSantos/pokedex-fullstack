package dev.guilhermeds.backend.domain.repository;

import dev.guilhermeds.backend.domain.exception.LocalPokemonNotFoundException;
import dev.guilhermeds.backend.domain.exception.NotFoundException;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPokemonRepositoryTest {

    // A repository with no records at all: enough to exercise the port's own default method.
    private final LocalPokemonRepository emptyRepository = new LocalPokemonRepository() {
        @Override
        public LocalPokemon save(LocalPokemon pokemon) {
            throw new UnsupportedOperationException("not used here");
        }

        @Override
        public Optional<LocalPokemon> findByPokedexNumber(PokedexNumber number) {
            return Optional.empty();
        }
    };

    @Test
    void shouldTurnAPokemonThatWasNeverSyncedIntoANotFoundError() {
        assertThatThrownBy(() -> emptyRepository.getByPokedexNumber(new PokedexNumber(25)))
            .isInstanceOf(LocalPokemonNotFoundException.class)
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Pokémon #25 is not in the local database");
    }
}
