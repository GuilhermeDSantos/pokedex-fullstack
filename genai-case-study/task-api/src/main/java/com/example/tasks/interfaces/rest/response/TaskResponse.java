package com.example.tasks.interfaces.rest.response;

import com.example.tasks.application.dto.TaskOutput;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    String status,
    LocalDate dueDate,
    Instant createdAt,
    Instant updatedAt,
    long version
) {

    public static TaskResponse from(TaskOutput task) {
        return new TaskResponse(task.id(), task.title(), task.description(), task.status(), task.dueDate(),
            task.createdAt(), task.updatedAt(), task.version());
    }
}
