package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokemonSummary;
import dev.guilhermeds.backend.domain.model.PokemonType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record PokemonSummaryOutput(int pokedexNumber, String name, String localizedName, String spriteUrl, String category,
                                   BigDecimal weightKilograms, List<String> types, List<AbilityOutput> abilities) {

    public static PokemonSummaryOutput from(PokemonSummary summary, Optional<LocalPokemon> local) {
        return new PokemonSummaryOutput(
            summary.number().value(),
            summary.name(),
            local.map(record -> record.getCustomAttributes().localizedName()).orElse(null),
            summary.spriteUrl(),
            summary.category(),
            summary.weight().kilograms(),
            summary.types().stream().map(PokemonType::name).toList(),
            summary.abilities().stream().map(AbilityOutput::from).toList());
    }
}
