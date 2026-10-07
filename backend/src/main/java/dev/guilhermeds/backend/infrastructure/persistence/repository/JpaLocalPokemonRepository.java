package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.domain.exception.LocalPokemonDataUnavailableException;
import dev.guilhermeds.backend.domain.exception.PokemonAlreadySyncedException;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.infrastructure.persistence.DatabaseFailures;
import dev.guilhermeds.backend.infrastructure.persistence.mapper.LocalPokemonEntityMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class JpaLocalPokemonRepository implements LocalPokemonRepository {

    private static final String UNIQUE_NUMBER_CONSTRAINT = "uk_local_pokemons_pokedex_number";

    private final LocalPokemonJpaRepository jpaRepository;
    private final LocalPokemonEntityMapper mapper;

    public JpaLocalPokemonRepository(LocalPokemonJpaRepository jpaRepository, LocalPokemonEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public LocalPokemon save(LocalPokemon pokemon) {
        try {
            return reachable(() -> mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(pokemon))));
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, UNIQUE_NUMBER_CONSTRAINT)) {
                throw new PokemonAlreadySyncedException(pokemon.getPokedexNumber());
            }
            throw exception;
        }
    }

    @Override
    public Optional<LocalPokemon> findByPokedexNumber(PokedexNumber number) {
        return reachable(() -> jpaRepository.findByPokedexNumber(number.value()).map(mapper::toDomain));
    }

    @Override
    public void delete(LocalPokemon pokemon) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private static <T> T reachable(Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException exception) {
            if (DatabaseFailures.isUnreachable(exception)) {
                throw new LocalPokemonDataUnavailableException(exception);
            }
            throw exception;
        }
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraint) {
        var cause = exception.getMostSpecificCause().getMessage();
        return cause != null && cause.contains(constraint);
    }
}
