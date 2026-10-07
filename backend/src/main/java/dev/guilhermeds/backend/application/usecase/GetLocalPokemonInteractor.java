package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetLocalPokemonInput;
import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;

public class GetLocalPokemonInteractor implements GetLocalPokemonUseCase {

    private final PokemonRepository pokemonRepository;
    private final LocalPokemonRepository localPokemonRepository;
    private final PokemonMapper mapper;

    public GetLocalPokemonInteractor(PokemonRepository pokemonRepository, LocalPokemonRepository localPokemonRepository,
                                     PokemonMapper mapper) {
        this.pokemonRepository = pokemonRepository;
        this.localPokemonRepository = localPokemonRepository;
        this.mapper = mapper;
    }

    @Override
    public LocalPokemonOutput execute(GetLocalPokemonInput input) {
        var number = mapper.toIdentifier(input.identifier()).asNumber();
        return LocalPokemonOutput.from(localPokemonRepository.getByPokedexNumber(number));
    }
}
