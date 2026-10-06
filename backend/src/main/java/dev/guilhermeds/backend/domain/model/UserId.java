package dev.guilhermeds.backend.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "user id must not be null");
    }

    // Called only at the edge (interfaces/), so use cases receive the id instead of inventing it.
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }
}
