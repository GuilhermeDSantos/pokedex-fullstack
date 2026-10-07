package dev.guilhermeds.backend.interfaces.rest.response;

import java.math.BigDecimal;
import java.util.List;

public record PokemonDetailResponse(int pokedexNumber, String name, String category, BigDecimal heightMeters,
                                    BigDecimal weightKilograms, String spriteUrl, String artworkUrl, List<String> types,
                                    List<AbilityResponse> abilities, List<StatResponse> stats, String description,
                                    EvolutionStageResponse evolutionChain) {
}
