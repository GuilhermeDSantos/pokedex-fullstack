package dev.guilhermeds.backend.domain.repository;

import dev.guilhermeds.backend.domain.exception.LocalPokemonNotFoundException;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokedexNumber;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Our records of Pokémon, keyed by Pokédex number: a name is resolved through the PokemonRepository first.
public interface LocalPokemonRepository {

    /** @throws dev.guilhermeds.backend.domain.exception.PokemonAlreadySyncedException if the number is taken */
    LocalPokemon save(LocalPokemon pokemon);

    Optional<LocalPokemon> findByPokedexNumber(PokedexNumber number);

    void delete(LocalPokemon pokemon);

    // A list page: the records of all its Pokémon in one call, never one per card.
    List<LocalPokemon> findAllByPokedexNumbers(Collection<PokedexNumber> numbers);

    default LocalPokemon getByPokedexNumber(PokedexNumber number) {
        return findByPokedexNumber(number).orElseThrow(() -> new LocalPokemonNotFoundException(number));
    }
}
