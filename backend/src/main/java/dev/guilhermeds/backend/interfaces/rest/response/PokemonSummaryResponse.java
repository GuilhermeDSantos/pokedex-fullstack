package dev.guilhermeds.backend.interfaces.rest.response;

import java.math.BigDecimal;
import java.util.List;

public record PokemonSummaryResponse(int pokedexNumber, String name, String spriteUrl, String category,
                                     BigDecimal weightKilograms, List<String> types, List<AbilityResponse> abilities) {
}
