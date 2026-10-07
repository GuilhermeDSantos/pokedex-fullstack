package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.model.UserAccount;

import java.time.Instant;
import java.util.UUID;

public record UserOutput(
    UUID id,
    String email,
    String name,
    Instant createdAt
) {

    public static UserOutput from(UserAccount account) {
        return new UserOutput(
            account.getId().value(),
            account.getEmail().value(),
            account.getName().value(),
            account.getCreatedAt());
    }
}
