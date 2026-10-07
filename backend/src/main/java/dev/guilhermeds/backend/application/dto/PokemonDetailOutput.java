package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.PokemonDetail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record PokemonDetailOutput(
    int pokedexNumber,
    String name,
    String category,
    BigDecimal heightMeters,
    BigDecimal weightKilograms,
    String spriteUrl,
    String artworkUrl,
    List<String> types,
    List<AbilityOutput> abilities,
    List<StatOutput> stats,
    String description,
    EvolutionStageOutput evolutionChain,
    LocalAttributesOutput local   // null when the Pokémon isn't synced
) {

    public static PokemonDetailOutput from(PokemonDetail detail, Optional<LocalPokemon> local) {
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
            EvolutionStageOutput.from(detail.evolutionChain()),
            local.map(LocalAttributesOutput::from).orElse(null));
    }
}
