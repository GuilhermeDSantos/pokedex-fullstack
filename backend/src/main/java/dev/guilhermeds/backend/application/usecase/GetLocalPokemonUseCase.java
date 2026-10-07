package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetLocalPokemonInput;
import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;

public interface GetLocalPokemonUseCase {
    LocalPokemonOutput execute(GetLocalPokemonInput input);
}
