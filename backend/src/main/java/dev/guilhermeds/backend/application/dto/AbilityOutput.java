package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.Ability;

public record AbilityOutput(String name, boolean hidden) {

    public static AbilityOutput from(Ability ability) {
        return new AbilityOutput(ability.name(), ability.hidden());
    }
}
