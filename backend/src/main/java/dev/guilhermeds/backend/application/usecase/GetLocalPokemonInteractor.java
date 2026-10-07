package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetLocalPokemonInput;
import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;

public class GetLocalPokemonInteractor implements GetLocalPokemonUseCase {

    private final LocalPokemonRepository localPokemonRepository;
    private final PokemonMapper mapper;

    public GetLocalPokemonInteractor(LocalPokemonRepository localPokemonRepository, PokemonMapper mapper) {
        this.localPokemonRepository = localPokemonRepository;
        this.mapper = mapper;
    }

    @Override
    public LocalPokemonOutput execute(GetLocalPokemonInput input) {
        var number = mapper.toPokedexNumber(input.pokedexNumber());
        return LocalPokemonOutput.from(localPokemonRepository.getByPokedexNumber(number));
    }
}
