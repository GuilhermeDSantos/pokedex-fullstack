package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.DeleteTaskInput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.application.port.UnitOfWork;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.domain.repository.TaskRepository;

public class DeleteTaskInteractor implements DeleteTaskUseCase {

    private final TaskRepository taskRepository;
    private final TaskMapper mapper;
    private final UnitOfWork unitOfWork;

    public DeleteTaskInteractor(TaskRepository taskRepository, TaskMapper mapper, UnitOfWork unitOfWork) {
        this.taskRepository = taskRepository;
        this.mapper = mapper;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void execute(DeleteTaskInput input, UserId owner) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
