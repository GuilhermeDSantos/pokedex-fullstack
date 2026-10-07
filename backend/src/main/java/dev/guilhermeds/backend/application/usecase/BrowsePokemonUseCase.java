package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.application.dto.PageOutput;
import dev.guilhermeds.backend.application.dto.PokemonSummaryOutput;

public interface BrowsePokemonUseCase {
    PageOutput<PokemonSummaryOutput> execute(BrowsePokemonInput input);
}
