package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.RemoveLocalPokemonInput;

public interface RemoveLocalPokemonUseCase {
    void execute(RemoveLocalPokemonInput input);
}
