package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.RemoveLocalPokemonInput;
import dev.guilhermeds.backend.application.mapper.PokemonMapper;
import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;

public class RemoveLocalPokemonInteractor implements RemoveLocalPokemonUseCase {

    private final LocalPokemonRepository localPokemonRepository;
    private final PokemonMapper mapper;
    private final UnitOfWork unitOfWork;

    public RemoveLocalPokemonInteractor(LocalPokemonRepository localPokemonRepository, PokemonMapper mapper,
                                        UnitOfWork unitOfWork) {
        this.localPokemonRepository = localPokemonRepository;
        this.mapper = mapper;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void execute(RemoveLocalPokemonInput input) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
