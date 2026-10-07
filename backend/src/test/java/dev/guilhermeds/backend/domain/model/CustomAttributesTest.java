package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

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
}
