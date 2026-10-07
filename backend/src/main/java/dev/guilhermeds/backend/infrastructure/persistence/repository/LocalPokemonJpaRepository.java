package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.infrastructure.persistence.entity.LocalPokemonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, UUID> {

    Optional<LocalPokemonEntity> findByPokedexNumber(int pokedexNumber);
}
