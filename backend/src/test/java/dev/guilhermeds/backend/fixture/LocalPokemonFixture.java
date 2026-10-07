package dev.guilhermeds.backend.fixture;

import dev.guilhermeds.backend.domain.model.LocalPokemonId;
import dev.guilhermeds.backend.domain.model.PokedexNumber;

import java.time.Instant;
import java.util.UUID;

public final class LocalPokemonFixture {

    public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    public static final LocalPokemonId PIKACHU_ID =
        new LocalPokemonId(UUID.fromString("00000000-0000-0000-0000-000000000025"));
    public static final PokedexNumber PIKACHU_NUMBER = new PokedexNumber(25);

    private LocalPokemonFixture() {
    }
}
