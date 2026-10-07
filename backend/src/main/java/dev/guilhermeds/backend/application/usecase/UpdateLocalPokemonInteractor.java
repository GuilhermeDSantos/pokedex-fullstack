package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;
import dev.guilhermeds.backend.application.dto.UpdateLocalPokemonInput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;

import java.time.Instant;

public class UpdateLocalPokemonInteractor implements UpdateLocalPokemonUseCase {

    private final LocalPokemonRepository localPokemonRepository;
    private final PokemonMapper mapper;
    private final UnitOfWork unitOfWork;

    public UpdateLocalPokemonInteractor(LocalPokemonRepository localPokemonRepository, PokemonMapper mapper,
                                        UnitOfWork unitOfWork) {
        this.localPokemonRepository = localPokemonRepository;
        this.mapper = mapper;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public LocalPokemonOutput execute(UpdateLocalPokemonInput input, Instant now) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
