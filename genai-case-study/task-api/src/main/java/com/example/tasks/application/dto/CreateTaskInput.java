package com.example.tasks.application.dto;

import java.time.LocalDate;

public record CreateTaskInput(
    String title,
    String description,
    String status,
    LocalDate dueDate
) {
}
