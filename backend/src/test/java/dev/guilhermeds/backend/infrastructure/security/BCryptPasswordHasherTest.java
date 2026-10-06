package dev.guilhermeds.backend.infrastructure.security;

import dev.guilhermeds.backend.domain.model.RawPassword;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void shouldMatchOnlyTheOriginalPassword() {
        var hash = hasher.hash(new RawPassword("pikachu1"));

        assertThat(hash.value()).doesNotContain("pikachu1");
        assertThat(hasher.matches(new RawPassword("pikachu1"), hash)).isTrue();
        assertThat(hasher.matches(new RawPassword("raichu26"), hash)).isFalse();
    }
}
