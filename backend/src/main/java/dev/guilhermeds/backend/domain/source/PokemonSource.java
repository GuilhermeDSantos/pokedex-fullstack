package dev.guilhermeds.backend.domain.source;

import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;

// PokeAPI as the domain sees it: no HTTP, no JSON.
public interface PokemonSource {

    /** @throws PokemonSourceUnavailableException when PokeAPI can't be reached */
    Page<PokemonSummary> findAll(PageRequest pageRequest);
}
