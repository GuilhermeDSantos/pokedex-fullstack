package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;
import dev.guilhermeds.backend.application.dto.SyncPokemonInput;
import dev.guilhermeds.backend.domain.model.LocalPokemonId;

import java.time.Instant;

public interface SyncPokemonUseCase {
    LocalPokemonOutput execute(SyncPokemonInput input, LocalPokemonId id, Instant now);
}
