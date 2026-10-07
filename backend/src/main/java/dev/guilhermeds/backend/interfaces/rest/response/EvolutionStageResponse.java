package dev.guilhermeds.backend.interfaces.rest.response;

import java.util.List;

public record EvolutionStageResponse(
    String speciesName,
    int pokedexNumber,
    List<EvolutionStageResponse> evolvesTo
) {
}
