package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetPokemonInput;
import dev.guilhermeds.backend.application.dto.PokemonDetailOutput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.domain.source.PokemonSource;

public class GetPokemonInteractor implements GetPokemonUseCase {

    private final PokemonSource source;
    private final PokemonMapper mapper;

    public GetPokemonInteractor(PokemonSource source, PokemonMapper mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public PokemonDetailOutput execute(GetPokemonInput input) {
        return PokemonDetailOutput.from(source.getByIdentifier(mapper.toIdentifier(input.identifier())));
    }
}
