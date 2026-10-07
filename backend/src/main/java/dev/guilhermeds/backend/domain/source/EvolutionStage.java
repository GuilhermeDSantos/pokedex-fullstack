package dev.guilhermeds.backend.domain.source;

import dev.guilhermeds.backend.domain.model.PokedexNumber;

import java.util.List;
import java.util.Objects;

// A tree, not a line: lineages branch (Eevee evolves eight ways).
public record EvolutionStage(String speciesName, PokedexNumber number, List<EvolutionStage> evolvesTo) {

    public EvolutionStage {
        Objects.requireNonNull(speciesName, "speciesName must not be null");
        Objects.requireNonNull(number, "number must not be null");
        evolvesTo = evolvesTo == null ? List.of() : List.copyOf(evolvesTo);
    }
}
