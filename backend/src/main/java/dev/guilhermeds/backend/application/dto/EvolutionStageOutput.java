package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.EvolutionStage;

import java.util.List;

public record EvolutionStageOutput(String speciesName, int pokedexNumber, List<EvolutionStageOutput> evolvesTo) {

    public static EvolutionStageOutput from(EvolutionStage stage) {
        return new EvolutionStageOutput(stage.speciesName(), stage.number().value(),
            stage.evolvesTo().stream().map(EvolutionStageOutput::from).toList());
    }
}
