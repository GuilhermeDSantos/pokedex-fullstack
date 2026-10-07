package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.source.PokemonDetail;

import java.math.BigDecimal;
import java.util.List;

public record PokemonDetailOutput(int pokedexNumber, String name, String category, BigDecimal heightMeters,
                                  BigDecimal weightKilograms, String spriteUrl, String artworkUrl, List<String> types,
                                  List<AbilityOutput> abilities, List<StatOutput> stats, String description,
                                  EvolutionStageOutput evolutionChain) {

    public static PokemonDetailOutput from(PokemonDetail detail) {
        var profile = detail.profile();
        return new PokemonDetailOutput(
            detail.number().value(),
            profile.name(),
            profile.category(),
            profile.height().meters(),
            profile.weight().kilograms(),
            profile.spriteUrl(),
            profile.artworkUrl(),
            profile.types().stream().map(PokemonType::name).toList(),
            profile.abilities().stream().map(AbilityOutput::from).toList(),
            profile.stats().stream().map(StatOutput::from).toList(),
            profile.description(),
            EvolutionStageOutput.from(detail.evolutionChain()));
    }
}
