package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetPokemonInput;
import dev.guilhermeds.backend.application.dto.PokemonDetailOutput;

public interface GetPokemonUseCase {
    PokemonDetailOutput execute(GetPokemonInput input);
}
