package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.TaskOutput;
import com.example.tasks.application.dto.UpdateTaskInput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.application.port.UnitOfWork;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.domain.repository.TaskRepository;

import java.time.Instant;
import java.time.LocalDate;

public class UpdateTaskInteractor implements UpdateTaskUseCase {

    private final TaskRepository taskRepository;
    private final TaskMapper mapper;
    private final UnitOfWork unitOfWork;

    public UpdateTaskInteractor(TaskRepository taskRepository, TaskMapper mapper, UnitOfWork unitOfWork) {
        this.taskRepository = taskRepository;
        this.mapper = mapper;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public TaskOutput execute(UpdateTaskInput input, UserId owner, LocalDate today, Instant now) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
