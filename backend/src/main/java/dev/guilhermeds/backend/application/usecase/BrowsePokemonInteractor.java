package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.application.dto.PageOutput;
import dev.guilhermeds.backend.application.dto.PokemonSummaryOutput;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.source.PokemonSource;

public class BrowsePokemonInteractor implements BrowsePokemonUseCase {

    private final PokemonSource source;

    public BrowsePokemonInteractor(PokemonSource source) {
        this.source = source;
    }

    @Override
    public PageOutput<PokemonSummaryOutput> execute(BrowsePokemonInput input) {
        var pageRequest = new PageRequest(input.page(), input.size());
        return PageOutput.from(source.findAll(pageRequest).map(PokemonSummaryOutput::from), pageRequest);
    }
}
