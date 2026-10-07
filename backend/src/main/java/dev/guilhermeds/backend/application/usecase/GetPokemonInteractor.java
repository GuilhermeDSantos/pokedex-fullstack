package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetPokemonInput;
import dev.guilhermeds.backend.application.dto.PokemonDetailOutput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;

public class GetPokemonInteractor implements GetPokemonUseCase {

    private final PokemonRepository pokemonRepository;
    private final PokemonMapper mapper;

    public GetPokemonInteractor(PokemonRepository pokemonRepository, PokemonMapper mapper) {
        this.pokemonRepository = pokemonRepository;
        this.mapper = mapper;
    }

    @Override
    public PokemonDetailOutput execute(GetPokemonInput input) {
        return PokemonDetailOutput.from(pokemonRepository.getByIdentifier(mapper.toIdentifier(input.identifier())));
    }
}
