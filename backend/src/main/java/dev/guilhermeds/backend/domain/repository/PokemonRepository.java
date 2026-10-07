package dev.guilhermeds.backend.domain.repository;

import dev.guilhermeds.backend.domain.exception.PokemonNotFoundException;
import dev.guilhermeds.backend.domain.model.PokemonDetail;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.model.PokemonSummary;
import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;

import java.util.Optional;

// The canonical Pokémon data. Read-only: it isn't ours to change, so there is no save.
public interface PokemonRepository {

    /** @throws PokemonDataUnavailableException when the data can't be reached */
    Page<PokemonSummary> findAll(PageRequest pageRequest);

    /** Empty when there is no such Pokémon. @throws PokemonDataUnavailableException when unreachable */
    Optional<PokemonDetail> findByIdentifier(PokemonIdentifier identifier);

    default PokemonDetail getByIdentifier(PokemonIdentifier identifier) {
        return findByIdentifier(identifier).orElseThrow(() -> new PokemonNotFoundException(identifier));
    }
}
