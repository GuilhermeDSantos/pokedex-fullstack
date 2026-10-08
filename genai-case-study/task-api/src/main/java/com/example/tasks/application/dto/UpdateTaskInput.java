package com.example.tasks.application.dto;

import java.time.LocalDate;

public record UpdateTaskInput(
    String id,
    String title,
    String description,
    String status,
    LocalDate dueDate,
    long version
) {
}
