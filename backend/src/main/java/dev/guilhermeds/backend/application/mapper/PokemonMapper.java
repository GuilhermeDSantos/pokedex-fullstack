package dev.guilhermeds.backend.application.mapper;

import dev.guilhermeds.backend.application.dto.UpdateLocalPokemonInput;
import dev.guilhermeds.backend.domain.model.CustomAttributes;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.model.Tag;

import java.util.Set;
import java.util.stream.Collectors;

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

    public CustomAttributes toCustomAttributes(UpdateLocalPokemonInput input) {
        var tags = input.tags() == null
            ? Set.<Tag>of()
            : input.tags().stream().map(Tag::new).collect(Collectors.toSet());
        return new CustomAttributes(input.localizedName(), input.region(), tags);
    }
}
