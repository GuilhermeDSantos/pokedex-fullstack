package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.TaskOutput;
import com.example.tasks.application.dto.UpdateTaskInput;
import com.example.tasks.domain.model.UserId;

import java.time.Instant;
import java.time.LocalDate;

public interface UpdateTaskUseCase {
    TaskOutput execute(UpdateTaskInput input, UserId owner, LocalDate today, Instant now);
}
