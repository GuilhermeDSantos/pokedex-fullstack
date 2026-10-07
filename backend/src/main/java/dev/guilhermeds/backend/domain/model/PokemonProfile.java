package dev.guilhermeds.backend.domain.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// The full view of a Pokémon (US-02). Category, images and description are nullable: PokeAPI has gaps.
public record PokemonProfile(String name, String category, Height height, Weight weight, String spriteUrl,
                             String artworkUrl, List<PokemonType> types, List<Ability> abilities, List<BaseStat> stats,
                             String description) {

    public PokemonProfile {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(height, "height must not be null");
        Objects.requireNonNull(weight, "weight must not be null");
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
        stats = stats.stream().sorted(Comparator.comparing(BaseStat::name)).toList();
    }
}
