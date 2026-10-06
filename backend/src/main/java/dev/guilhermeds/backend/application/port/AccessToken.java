package dev.guilhermeds.backend.application.port;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
