package dev.guilhermeds.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties("security.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                "security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
    }
}
