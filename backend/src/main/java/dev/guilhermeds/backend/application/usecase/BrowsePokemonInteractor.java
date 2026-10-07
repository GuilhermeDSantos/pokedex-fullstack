package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.application.dto.PageOutput;
import dev.guilhermeds.backend.application.dto.PokemonSummaryOutput;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;

public class BrowsePokemonInteractor implements BrowsePokemonUseCase {

    private final PokemonRepository pokemonRepository;
    private final LocalPokemonRepository localPokemonRepository;

    public BrowsePokemonInteractor(PokemonRepository pokemonRepository, LocalPokemonRepository localPokemonRepository) {
        this.pokemonRepository = pokemonRepository;
        this.localPokemonRepository = localPokemonRepository;
    }

    @Override
    public PageOutput<PokemonSummaryOutput> execute(BrowsePokemonInput input) {
        var pageRequest = new PageRequest(input.page(), input.size());
        return PageOutput.from(pokemonRepository.findAll(pageRequest).map(PokemonSummaryOutput::from), pageRequest);
    }
}
