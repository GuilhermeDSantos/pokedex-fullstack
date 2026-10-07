package dev.guilhermeds.backend.interfaces.rest.mapper;

import dev.guilhermeds.backend.application.dto.AbilityOutput;
import dev.guilhermeds.backend.application.dto.PageOutput;
import dev.guilhermeds.backend.application.dto.PokemonSummaryOutput;
import dev.guilhermeds.backend.interfaces.rest.response.AbilityResponse;
import dev.guilhermeds.backend.interfaces.rest.response.PageResponse;
import dev.guilhermeds.backend.interfaces.rest.response.PokemonSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class PokemonRestMapper {

    public PageResponse<PokemonSummaryResponse> toPageResponse(PageOutput<PokemonSummaryOutput> page) {
        return new PageResponse<>(page.content().stream().map(this::toResponse).toList(),
            page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    private PokemonSummaryResponse toResponse(PokemonSummaryOutput card) {
        return new PokemonSummaryResponse(card.pokedexNumber(), card.name(), card.spriteUrl(), card.category(),
            card.weightKilograms(), card.types(), card.abilities().stream().map(this::toResponse).toList());
    }

    private AbilityResponse toResponse(AbilityOutput ability) {
        return new AbilityResponse(ability.name(), ability.hidden());
    }
}
