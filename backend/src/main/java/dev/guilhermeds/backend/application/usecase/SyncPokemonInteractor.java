package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;
import dev.guilhermeds.backend.application.dto.SyncPokemonInput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.domain.exception.PokemonAlreadySyncedException;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.LocalPokemonId;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;

import java.time.Instant;

public class SyncPokemonInteractor implements SyncPokemonUseCase {

    private final PokemonRepository pokemonRepository;
    private final LocalPokemonRepository localPokemonRepository;
    private final PokemonMapper mapper;
    private final UnitOfWork unitOfWork;

    public SyncPokemonInteractor(PokemonRepository pokemonRepository, LocalPokemonRepository localPokemonRepository,
                                 PokemonMapper mapper, UnitOfWork unitOfWork) {
        this.pokemonRepository = pokemonRepository;
        this.localPokemonRepository = localPokemonRepository;
        this.mapper = mapper;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public LocalPokemonOutput execute(SyncPokemonInput input, LocalPokemonId id, Instant now) {
        // Read the canonical data before the transaction: a slow call must never hold a connection.
        var number = pokemonRepository.getByIdentifier(mapper.toIdentifier(input.identifier())).number();

        return unitOfWork.inTransaction(() -> {
            localPokemonRepository.findByPokedexNumber(number).ifPresent(existing -> {
                throw new PokemonAlreadySyncedException(number);
            });
            var synced = localPokemonRepository.save(LocalPokemon.create(id, number, now));
            return LocalPokemonOutput.from(synced);
        });
    }
}
