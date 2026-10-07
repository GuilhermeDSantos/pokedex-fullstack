package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAttributesTest {

    // A Pokémon is synced with none of our own fields yet: they are filled in later (US-04).
    @Test
    void shouldStartEmpty() {
        var empty = CustomAttributes.empty();

        assertThat(empty.localizedName()).isNull();
        assertThat(empty.region()).isNull();
        assertThat(empty.tags()).isEmpty();
    }

    // An emptied field in the form means "not set", not a blank value to display.
    @Test
    void shouldTrimTheTextsAndTreatBlankAsNotSet() {
        var attributes = new CustomAttributes("  Pica ", "   ", Set.of());

        assertThat(attributes.localizedName()).isEqualTo("Pica");
        assertThat(attributes.region()).isNull();
    }
}
