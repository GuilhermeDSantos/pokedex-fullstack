package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.fixture.LocalPokemonFixture;
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

    // PUT semantics (US-04): the form sends all of our fields, so what it leaves out is cleared.
    @Test
    void shouldReplaceAllOfOurFieldsAndRecordWhenTheyChanged() {
        var pikachu = LocalPokemonFixture.renamedPikachu();
        var later = NOW.plusSeconds(3600);
        var edited = new CustomAttributes(null, "Johto", Set.of(new Tag("electric-mouse")));

        pikachu.updateCustomAttributes(edited, later);

        assertThat(pikachu.getCustomAttributes()).isEqualTo(edited);
        assertThat(pikachu.getUpdatedAt()).isEqualTo(later);
        assertThat(pikachu.getSyncedAt()).isEqualTo(NOW);
    }
}
