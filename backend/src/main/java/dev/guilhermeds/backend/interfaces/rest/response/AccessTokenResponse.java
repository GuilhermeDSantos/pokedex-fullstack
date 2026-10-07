package dev.guilhermeds.backend.interfaces.rest.response;

import java.time.Instant;

public record AccessTokenResponse(
    String accessToken,
    String tokenType,
    Instant expiresAt
) {
}
