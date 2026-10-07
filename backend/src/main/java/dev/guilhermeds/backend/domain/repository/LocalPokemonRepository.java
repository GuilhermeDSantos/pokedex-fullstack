package dev.guilhermeds.backend.domain.repository;

import dev.guilhermeds.backend.domain.exception.LocalPokemonNotFoundException;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokedexNumber;

import java.util.Optional;

// Our records of Pokémon, keyed by Pokédex number: a name is resolved through the PokemonRepository first.
public interface LocalPokemonRepository {

    /** @throws dev.guilhermeds.backend.domain.exception.PokemonAlreadySyncedException if the number is taken */
    LocalPokemon save(LocalPokemon pokemon);

    Optional<LocalPokemon> findByPokedexNumber(PokedexNumber number);

    default LocalPokemon getByPokedexNumber(PokedexNumber number) {
        return findByPokedexNumber(number).orElseThrow(() -> new LocalPokemonNotFoundException(number));
    }
}
