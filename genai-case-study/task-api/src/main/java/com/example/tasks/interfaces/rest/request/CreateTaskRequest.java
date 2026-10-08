package com.example.tasks.interfaces.rest.request;

import com.example.tasks.domain.model.Description;
import com.example.tasks.domain.model.Title;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Sizes only, for a message per field; every other rule belongs to the domain.
public record CreateTaskRequest(
    @Size(max = Title.MAX_LENGTH) String title,
    @Size(max = Description.MAX_LENGTH) String description,
    String status,
    LocalDate dueDate
) {
}
