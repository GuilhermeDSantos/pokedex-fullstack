package dev.guilhermeds.backend.interfaces.rest.mapper;

import dev.guilhermeds.backend.application.dto.AbilityOutput;
import dev.guilhermeds.backend.application.dto.EvolutionStageOutput;
import dev.guilhermeds.backend.application.dto.LocalAttributesOutput;
import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;
import dev.guilhermeds.backend.application.dto.PageOutput;
import dev.guilhermeds.backend.application.dto.PokemonDetailOutput;
import dev.guilhermeds.backend.application.dto.PokemonSummaryOutput;
import dev.guilhermeds.backend.application.dto.StatOutput;
import dev.guilhermeds.backend.application.dto.UpdateLocalPokemonInput;
import dev.guilhermeds.backend.interfaces.rest.request.UpdateLocalPokemonRequest;
import dev.guilhermeds.backend.interfaces.rest.response.AbilityResponse;
import dev.guilhermeds.backend.interfaces.rest.response.EvolutionStageResponse;
import dev.guilhermeds.backend.interfaces.rest.response.LocalAttributesResponse;
import dev.guilhermeds.backend.interfaces.rest.response.LocalPokemonResponse;
import dev.guilhermeds.backend.interfaces.rest.response.PageResponse;
import dev.guilhermeds.backend.interfaces.rest.response.PokemonDetailResponse;
import dev.guilhermeds.backend.interfaces.rest.response.PokemonSummaryResponse;
import dev.guilhermeds.backend.interfaces.rest.response.StatResponse;
import org.springframework.stereotype.Component;

@Component
public class PokemonRestMapper {

    public PageResponse<PokemonSummaryResponse> toPageResponse(PageOutput<PokemonSummaryOutput> page) {
        return new PageResponse<>(page.content().stream().map(this::toResponse).toList(),
            page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    public PokemonDetailResponse toResponse(PokemonDetailOutput pokemon) {
        return new PokemonDetailResponse(pokemon.pokedexNumber(), pokemon.name(), pokemon.category(),
            pokemon.heightMeters(), pokemon.weightKilograms(), pokemon.spriteUrl(), pokemon.artworkUrl(), pokemon.types(),
            pokemon.abilities().stream().map(this::toResponse).toList(),
            pokemon.stats().stream().map(this::toResponse).toList(),
            pokemon.description(), toResponse(pokemon.evolutionChain()),
            pokemon.local() == null ? null : toResponse(pokemon.local()));
    }

    public UpdateLocalPokemonInput toInput(String pokedexNumber, UpdateLocalPokemonRequest request) {
        return new UpdateLocalPokemonInput(pokedexNumber, request.localizedName(), request.region(), request.tags());
    }

    public LocalPokemonResponse toResponse(LocalPokemonOutput local) {
        return new LocalPokemonResponse(local.pokedexNumber(), local.localizedName(), local.region(), local.tags(),
            local.syncedAt(), local.updatedAt());
    }

    private LocalAttributesResponse toResponse(LocalAttributesOutput local) {
        return new LocalAttributesResponse(local.localizedName(), local.region(), local.tags(), local.syncedAt(),
            local.updatedAt());
    }

    private StatResponse toResponse(StatOutput stat) {
        return new StatResponse(stat.name(), stat.value());
    }

    private EvolutionStageResponse toResponse(EvolutionStageOutput stage) {
        return new EvolutionStageResponse(stage.speciesName(), stage.pokedexNumber(),
            stage.evolvesTo().stream().map(this::toResponse).toList());
    }

    private PokemonSummaryResponse toResponse(PokemonSummaryOutput card) {
        return new PokemonSummaryResponse(card.pokedexNumber(), card.name(), card.spriteUrl(), card.category(),
            card.weightKilograms(), card.types(), card.abilities().stream().map(this::toResponse).toList());
    }

    private AbilityResponse toResponse(AbilityOutput ability) {
        return new AbilityResponse(ability.name(), ability.hidden());
    }
}
