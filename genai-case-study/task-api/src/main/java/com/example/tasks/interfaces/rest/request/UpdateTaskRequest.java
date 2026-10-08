package com.example.tasks.interfaces.rest.request;

import com.example.tasks.domain.model.Description;
import com.example.tasks.domain.model.Title;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaskRequest(
    @Size(max = Title.MAX_LENGTH) String title,
    @Size(max = Description.MAX_LENGTH) String description,
    String status,
    LocalDate dueDate,
    @NotNull Long version
) {
}
