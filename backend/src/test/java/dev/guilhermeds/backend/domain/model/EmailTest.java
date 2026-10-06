package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTest {

    @Test
    void shouldTrimAndLowerCaseTheAddress() {
        assertThat(new Email("  Ash.Ketchum@Pallet.Town ").value()).isEqualTo("ash.ketchum@pallet.town");
    }
}
