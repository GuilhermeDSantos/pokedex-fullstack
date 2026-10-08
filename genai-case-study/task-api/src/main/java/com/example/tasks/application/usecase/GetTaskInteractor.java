package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.GetTaskInput;
import com.example.tasks.application.dto.TaskOutput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.domain.repository.TaskRepository;

public class GetTaskInteractor implements GetTaskUseCase {

    private final TaskRepository taskRepository;
    private final TaskMapper mapper;

    public GetTaskInteractor(TaskRepository taskRepository, TaskMapper mapper) {
        this.taskRepository = taskRepository;
        this.mapper = mapper;
    }

    @Override
    public TaskOutput execute(GetTaskInput input, UserId owner) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
