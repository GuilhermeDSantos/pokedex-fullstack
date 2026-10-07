package dev.guilhermeds.backend.domain.repository;

import dev.guilhermeds.backend.domain.exception.NotFoundException;
import dev.guilhermeds.backend.domain.exception.PokemonNotFoundException;
import dev.guilhermeds.backend.domain.model.PokemonDetail;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.model.PokemonSummary;
import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonRepositoryTest {

    // A repository that knows no Pokémon at all: enough to exercise the port's own default method.
    private final PokemonRepository emptySource = new PokemonRepository() {
        @Override
        public Page<PokemonSummary> findAll(PageRequest pageRequest) {
            throw new UnsupportedOperationException("not used here");
        }

        @Override
        public Optional<PokemonDetail> findByIdentifier(PokemonIdentifier identifier) {
            return Optional.empty();
        }
    };

    @Test
    void shouldTurnAnUnknownPokemonIntoANotFoundError() {
        assertThatThrownBy(() -> emptySource.getByIdentifier(new PokemonIdentifier("missingno")))
            .isInstanceOf(PokemonNotFoundException.class)
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Pokémon 'missingno' was not found");
    }
}
