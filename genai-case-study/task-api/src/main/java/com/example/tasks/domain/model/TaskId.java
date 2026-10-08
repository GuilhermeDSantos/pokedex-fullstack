package com.example.tasks.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TaskId(UUID value) {

    public TaskId {
        Objects.requireNonNull(value, "value must not be null");
    }

    // Called at the edge only: business code receives ids, it never makes them.
    public static TaskId generate() {
        return new TaskId(UUID.randomUUID());
    }
}
