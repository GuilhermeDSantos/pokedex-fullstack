package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.application.port.AccessToken;

import java.time.Instant;

public record AccessTokenOutput(String accessToken, Instant expiresAt) {

    public static AccessTokenOutput from(AccessToken token) {
        return new AccessTokenOutput(token.value(), token.expiresAt());
    }
}
