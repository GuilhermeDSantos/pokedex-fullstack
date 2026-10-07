package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.application.dto.PageOutput;
import dev.guilhermeds.backend.application.dto.PokemonSummaryOutput;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokemonSummary;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        var page = pokemonRepository.findAll(pageRequest);
        var numbers = page.content().stream().map(PokemonSummary::number).toList();
        var ours = localPokemonRepository.findAllByPokedexNumbers(numbers).stream()
            .collect(Collectors.toMap(LocalPokemon::getPokedexNumber, Function.identity()));
        return PageOutput.from(
            page.map(summary -> PokemonSummaryOutput.from(summary, Optional.ofNullable(ours.get(summary.number())))),
            pageRequest);
    }
}
