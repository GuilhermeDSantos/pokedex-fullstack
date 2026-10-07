package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetPokemonInput;
import dev.guilhermeds.backend.application.dto.PokemonDetailOutput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;

public class GetPokemonInteractor implements GetPokemonUseCase {

    private final PokemonRepository pokemonRepository;
    private final LocalPokemonRepository localPokemonRepository;
    private final PokemonMapper mapper;

    public GetPokemonInteractor(PokemonRepository pokemonRepository, LocalPokemonRepository localPokemonRepository,
                                PokemonMapper mapper) {
        this.pokemonRepository = pokemonRepository;
        this.localPokemonRepository = localPokemonRepository;
        this.mapper = mapper;
    }

    @Override
    public PokemonDetailOutput execute(GetPokemonInput input) {
        var detail = pokemonRepository.getByIdentifier(mapper.toIdentifier(input.identifier()));
        // Absent means not synced, which is not an error.
        return PokemonDetailOutput.from(detail, localPokemonRepository.findByPokedexNumber(detail.number()));
    }
}
