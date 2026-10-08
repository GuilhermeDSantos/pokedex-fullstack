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
        var id = mapper.toTaskId(input.id());
        var title = mapper.toTitle(input.title());
        var description = mapper.toDescription(input.description());
        var status = mapper.toStatus(input.status());
        return unitOfWork.inTransaction(() -> {
            var task = taskRepository.getByIdAndOwner(id, owner);
            task.update(title, description, status, input.dueDate(), input.version(), today, now);
            return TaskOutput.from(taskRepository.save(task));
        });
    }
}
