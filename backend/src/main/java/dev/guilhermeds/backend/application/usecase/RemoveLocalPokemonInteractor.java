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
        var number = mapper.toPokedexNumber(input.pokedexNumber());
        unitOfWork.inTransaction(() -> {
            // Found first: removing what was never synced is a 404, not a 204 that claims it worked.
            localPokemonRepository.delete(localPokemonRepository.getByPokedexNumber(number));
        });
    }
}
