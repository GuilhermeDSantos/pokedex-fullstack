package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

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
}
