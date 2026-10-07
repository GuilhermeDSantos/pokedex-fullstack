package dev.guilhermeds.backend.domain.source;

import dev.guilhermeds.backend.domain.exception.PokemonNotFoundException;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;

import java.util.Optional;

// PokeAPI as the domain sees it: no HTTP, no JSON.
public interface PokemonSource {

    /** @throws PokemonSourceUnavailableException when PokeAPI can't be reached */
    Page<PokemonSummary> findAll(PageRequest pageRequest);

    /** Empty when PokeAPI doesn't know the Pokémon. @throws PokemonSourceUnavailableException when unreachable */
    Optional<PokemonDetail> findByIdentifier(PokemonIdentifier identifier);

    default PokemonDetail getByIdentifier(PokemonIdentifier identifier) {
        return findByIdentifier(identifier).orElseThrow(() -> new PokemonNotFoundException(identifier));
    }
}
