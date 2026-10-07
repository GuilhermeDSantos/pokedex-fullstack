package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.infrastructure.persistence.entity.LocalPokemonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, UUID> {

    Optional<LocalPokemonEntity> findByPokedexNumber(int pokedexNumber);

    // The tags come in the same query: a list page loads up to 50 records at once.
    @Query("select pokemon from LocalPokemonEntity pokemon left join fetch pokemon.tags where pokemon.pokedexNumber in :numbers")
    List<LocalPokemonEntity> findAllWithTagsByPokedexNumberIn(@Param("numbers") Collection<Integer> numbers);
}
