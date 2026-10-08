package com.example.tasks.application.dto;

public record ListTasksInput(
    String status,
    int page,
    int size
) {
}
