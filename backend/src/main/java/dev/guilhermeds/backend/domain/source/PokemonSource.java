package dev.guilhermeds.backend.domain.source;

import dev.guilhermeds.backend.domain.exception.PokemonNotFoundException;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;

import java.util.Optional;

// Where the canonical Pokémon data is read from. The domain doesn't know or care what is behind it.
public interface PokemonSource {

    /** @throws PokemonSourceUnavailableException when the source can't be reached */
    Page<PokemonSummary> findAll(PageRequest pageRequest);

    /** Empty when the source doesn't know the Pokémon. @throws PokemonSourceUnavailableException when unreachable */
    Optional<PokemonDetail> findByIdentifier(PokemonIdentifier identifier);

    default PokemonDetail getByIdentifier(PokemonIdentifier identifier) {
        return findByIdentifier(identifier).orElseThrow(() -> new PokemonNotFoundException(identifier));
    }
}
