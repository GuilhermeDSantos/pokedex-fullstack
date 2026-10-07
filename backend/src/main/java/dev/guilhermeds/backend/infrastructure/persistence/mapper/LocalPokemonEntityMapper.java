package dev.guilhermeds.backend.infrastructure.persistence.mapper;

import dev.guilhermeds.backend.domain.model.CustomAttributes;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.LocalPokemonId;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.Tag;
import dev.guilhermeds.backend.infrastructure.persistence.entity.LocalPokemonEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class LocalPokemonEntityMapper {

    public LocalPokemonEntity toEntity(LocalPokemon pokemon) {
        var entity = new LocalPokemonEntity();
        entity.setId(pokemon.getId().value());
        return copyInto(pokemon, entity);
    }

    public LocalPokemonEntity copyInto(LocalPokemon pokemon, LocalPokemonEntity entity) {
        var custom = pokemon.getCustomAttributes();
        entity.setPokedexNumber(pokemon.getPokedexNumber().value());
        entity.setLocalizedName(custom.localizedName());
        entity.setRegion(custom.region());
        // Same collection instance: Hibernate tracks the one it handed out.
        entity.getTags().clear();
        entity.getTags().addAll(custom.tags().stream().map(Tag::value).toList());
        entity.setSyncedAt(pokemon.getSyncedAt());
        entity.setUpdatedAt(pokemon.getUpdatedAt());
        return entity;
    }

    public LocalPokemon toDomain(LocalPokemonEntity entity) {
        return LocalPokemon.builder()
            .id(new LocalPokemonId(entity.getId()))
            .pokedexNumber(new PokedexNumber(entity.getPokedexNumber()))
            .customAttributes(new CustomAttributes(entity.getLocalizedName(), entity.getRegion(),
                entity.getTags().stream().map(Tag::new).collect(Collectors.toUnmodifiableSet())))
            .syncedAt(entity.getSyncedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
