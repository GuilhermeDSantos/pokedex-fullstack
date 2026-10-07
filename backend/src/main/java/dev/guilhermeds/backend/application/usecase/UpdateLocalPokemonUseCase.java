package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;
import dev.guilhermeds.backend.application.dto.UpdateLocalPokemonInput;

import java.time.Instant;

public interface UpdateLocalPokemonUseCase {
    LocalPokemonOutput execute(UpdateLocalPokemonInput input, Instant now);
}
