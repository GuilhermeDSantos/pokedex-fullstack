package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.CreateTaskInput;
import com.example.tasks.application.dto.TaskOutput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.application.port.UnitOfWork;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.domain.repository.TaskRepository;

import java.time.Instant;
import java.time.LocalDate;

public class CreateTaskInteractor implements CreateTaskUseCase {

    private final TaskRepository taskRepository;
    private final TaskMapper mapper;
    private final UnitOfWork unitOfWork;

    public CreateTaskInteractor(TaskRepository taskRepository, TaskMapper mapper, UnitOfWork unitOfWork) {
        this.taskRepository = taskRepository;
        this.mapper = mapper;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public TaskOutput execute(CreateTaskInput input, UserId owner, TaskId id, LocalDate today, Instant now) {
        // Built before the transaction: invalid input is a 400 that never opens one.
        var title = mapper.toTitle(input.title());
        var description = mapper.toDescription(input.description());
        var status = mapper.toStatus(input.status());
        var task = Task.create(id, owner, title, description, status, input.dueDate(), today, now);
        return unitOfWork.inTransaction(() -> {
            return TaskOutput.from(taskRepository.save(task));
        });
    }
}
