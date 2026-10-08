package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.CreateTaskInput;
import com.example.tasks.application.dto.TaskOutput;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.UserId;

import java.time.Instant;
import java.time.LocalDate;

public interface CreateTaskUseCase {
    TaskOutput execute(CreateTaskInput input, UserId owner, TaskId id, LocalDate today, Instant now);
}
