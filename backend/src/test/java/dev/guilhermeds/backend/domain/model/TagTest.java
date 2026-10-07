package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagTest {

    // "Starter" and "starter " are the same classification, so they are stored the same way.
    @Test
    void shouldTrimAndLowerCaseTheTag() {
        assertThat(new Tag("  Starter ").value()).isEqualTo("starter");
    }
}
