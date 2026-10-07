package dev.guilhermeds.backend.application.mapper;

import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;

public class PokemonMapper {

    public PokemonIdentifier toIdentifier(String raw) {
        return new PokemonIdentifier(raw);
    }

    public PokemonIdentifier toIdentifier(PokedexNumber number) {
        return new PokemonIdentifier(String.valueOf(number.value()));
    }

    public PokedexNumber toPokedexNumber(String raw) {
        return PokedexNumber.parse(raw);
    }
}
