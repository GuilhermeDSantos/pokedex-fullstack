package dev.guilhermeds.backend.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void shouldRefuseASecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofHours(1), "pokemon-catalog"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("security.jwt.secret must be at least 32 bytes for HS256");
    }
}
