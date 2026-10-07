package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonProfile;

import java.util.Objects;

// One Pokémon in full (US-02): its profile and the lineage it belongs to.
public record PokemonDetail(PokedexNumber number, PokemonProfile profile, EvolutionStage evolutionChain) {

    public PokemonDetail {
        Objects.requireNonNull(number, "number must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(evolutionChain, "evolutionChain must not be null");
    }
}
