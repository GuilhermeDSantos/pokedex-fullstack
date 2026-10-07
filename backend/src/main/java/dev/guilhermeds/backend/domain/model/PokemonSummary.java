package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.model.Ability;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.Weight;

import java.util.List;
import java.util.Objects;

// One list card (US-01): sprite, category, mass and skills. Sprite and category are nullable: the canonical data has gaps.
public record PokemonSummary(
    PokedexNumber number,
    String name,
    String spriteUrl,
    String category,
    Weight weight,
    List<PokemonType> types,
    List<Ability> abilities
) {

    public PokemonSummary {
        Objects.requireNonNull(number, "number must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(weight, "weight must not be null");
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
    }
}
