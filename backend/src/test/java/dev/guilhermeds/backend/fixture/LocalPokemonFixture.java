package dev.guilhermeds.backend.fixture;

import dev.guilhermeds.backend.domain.model.CustomAttributes;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.LocalPokemonId;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.Tag;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class LocalPokemonFixture {

    public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    public static final LocalPokemonId PIKACHU_ID =
        new LocalPokemonId(UUID.fromString("00000000-0000-0000-0000-000000000025"));
    public static final PokedexNumber PIKACHU_NUMBER = new PokedexNumber(25);

    private LocalPokemonFixture() {
    }

    public static LocalPokemon syncedPikachu() {
        return LocalPokemon.create(PIKACHU_ID, PIKACHU_NUMBER, NOW);
    }

    // A record whose own fields were already filled in: rebuilt as the mapper would.
    public static LocalPokemon renamedPikachu() {
        return LocalPokemon.builder()
            .id(PIKACHU_ID)
            .pokedexNumber(PIKACHU_NUMBER)
            .customAttributes(new CustomAttributes("Pica", "Kanto", Set.of(new Tag("starter"), new Tag("mascot"))))
            .syncedAt(NOW)
            .updatedAt(NOW.plusSeconds(60))
            .build();
    }
}
