package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.NOW;
import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.PIKACHU_ID;
import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.PIKACHU_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;

class LocalPokemonTest {

    // The Pokédex number is the whole link to the canonical data; the name always comes from there (D-039).
    @Test
    void shouldBeSyncedWithItsNumberAndNoneOfOurFieldsYet() {
        var pikachu = LocalPokemon.create(PIKACHU_ID, PIKACHU_NUMBER, NOW);

        assertThat(pikachu.getId()).isEqualTo(PIKACHU_ID);
        assertThat(pikachu.getPokedexNumber()).isEqualTo(PIKACHU_NUMBER);
        assertThat(pikachu.getCustomAttributes()).isEqualTo(CustomAttributes.empty());
        assertThat(pikachu.getSyncedAt()).isEqualTo(NOW);
        assertThat(pikachu.getUpdatedAt()).isEqualTo(NOW);
    }

    // Like a nickname: shown as "Pica", still Pikachu underneath.
    @Test
    void shouldDisplayTheLocalizedNameWhenThereIsOneAndTheCanonicalNameOtherwise() {
        var synced = LocalPokemon.create(PIKACHU_ID, PIKACHU_NUMBER, NOW);
        var renamed = LocalPokemon.builder()
            .id(PIKACHU_ID)
            .pokedexNumber(PIKACHU_NUMBER)
            .customAttributes(new CustomAttributes("Pica", null, Set.of()))
            .syncedAt(NOW)
            .updatedAt(NOW)
            .build();

        assertThat(synced.displayName("pikachu")).isEqualTo("pikachu");
        assertThat(renamed.displayName("pikachu")).isEqualTo("Pica");
    }
}
