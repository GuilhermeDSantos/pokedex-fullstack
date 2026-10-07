package dev.guilhermeds.backend.application.mapper;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;

public class PokemonMapper {

    public PokemonIdentifier toIdentifier(String raw) {
        return new PokemonIdentifier(raw);
    }

    public PokedexNumber toPokedexNumber(String raw) {
        return PokedexNumber.parse(raw);
    }
}
