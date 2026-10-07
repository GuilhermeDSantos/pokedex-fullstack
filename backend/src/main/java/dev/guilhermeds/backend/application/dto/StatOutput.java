package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.BaseStat;

public record StatOutput(String name, int value) {

    public static StatOutput from(BaseStat stat) {
        return new StatOutput(stat.name().name(), stat.value());
    }
}
