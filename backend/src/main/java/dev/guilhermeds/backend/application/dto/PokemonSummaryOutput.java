package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.PokemonSummary;

import java.math.BigDecimal;
import java.util.List;

public record PokemonSummaryOutput(int pokedexNumber, String name, String localizedName, String spriteUrl, String category,
                                   BigDecimal weightKilograms, List<String> types, List<AbilityOutput> abilities) {

    public static PokemonSummaryOutput from(PokemonSummary summary) {
        return new PokemonSummaryOutput(
            summary.number().value(),
            summary.name(),
            null,
            summary.spriteUrl(),
            summary.category(),
            summary.weight().kilograms(),
            summary.types().stream().map(PokemonType::name).toList(),
            summary.abilities().stream().map(AbilityOutput::from).toList());
    }
}
