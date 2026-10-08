package com.example.tasks.application.dto;

import com.example.tasks.domain.model.Task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskOutput(
    UUID id,
    String title,
    String description,
    String status,
    LocalDate dueDate,
    Instant createdAt,
    Instant updatedAt,
    long version
) {

    public static TaskOutput from(Task task) {
        return new TaskOutput(
            task.getId().value(),
            task.getTitle().value(),
            task.getDescription() == null ? null : task.getDescription().value(),
            task.getStatus().name(),
            task.getDueDate(),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            task.getVersion());
    }
}
