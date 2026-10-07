package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import dev.guilhermeds.backend.infrastructure.persistence.mapper.LocalPokemonEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaLocalPokemonRepository implements LocalPokemonRepository {

    private final LocalPokemonJpaRepository jpaRepository;
    private final LocalPokemonEntityMapper mapper;

    public JpaLocalPokemonRepository(LocalPokemonJpaRepository jpaRepository, LocalPokemonEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public LocalPokemon save(LocalPokemon pokemon) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(pokemon)));
    }

    @Override
    public Optional<LocalPokemon> findByPokedexNumber(PokedexNumber number) {
        return jpaRepository.findByPokedexNumber(number.value()).map(mapper::toDomain);
    }
}
