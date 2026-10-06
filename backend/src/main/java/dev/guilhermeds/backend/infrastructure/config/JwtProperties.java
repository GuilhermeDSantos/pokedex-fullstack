package dev.guilhermeds.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("security.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {
}
