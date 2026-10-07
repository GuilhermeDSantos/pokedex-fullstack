package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.NOW;
import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.PIKACHU_ID;
import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.PIKACHU_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;

class LocalPokemonTest {

    @Test
    void shouldBeSyncedWithItsIdentityAndNoneOfOurFieldsYet() {
        var pikachu = LocalPokemon.create(PIKACHU_ID, PIKACHU_NUMBER, "pikachu", NOW);

        assertThat(pikachu.getId()).isEqualTo(PIKACHU_ID);
        assertThat(pikachu.getPokedexNumber()).isEqualTo(PIKACHU_NUMBER);
        assertThat(pikachu.getName()).isEqualTo("pikachu");
        assertThat(pikachu.getCustomAttributes()).isEqualTo(CustomAttributes.empty());
        assertThat(pikachu.getSyncedAt()).isEqualTo(NOW);
        assertThat(pikachu.getUpdatedAt()).isEqualTo(NOW);
    }

    // Like a nickname: shown as "Pica", still Pikachu underneath (D-039).
    @Test
    void shouldDisplayTheLocalizedNameWhenThereIsOneAndTheOriginalOtherwise() {
        var synced = LocalPokemon.create(PIKACHU_ID, PIKACHU_NUMBER, "pikachu", NOW);
        var renamed = LocalPokemon.builder()
            .id(PIKACHU_ID)
            .pokedexNumber(PIKACHU_NUMBER)
            .name("pikachu")
            .customAttributes(new CustomAttributes("Pica", null, Set.of()))
            .syncedAt(NOW)
            .updatedAt(NOW)
            .build();

        assertThat(synced.displayName()).isEqualTo("pikachu");
        assertThat(renamed.displayName()).isEqualTo("Pica");
        assertThat(renamed.getName()).isEqualTo("pikachu");
    }
}
