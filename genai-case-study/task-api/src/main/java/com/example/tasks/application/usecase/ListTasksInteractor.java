package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.ListTasksInput;
import com.example.tasks.application.dto.PageOutput;
import com.example.tasks.application.dto.TaskOutput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.domain.repository.TaskRepository;

public class ListTasksInteractor implements ListTasksUseCase {

    private final TaskRepository taskRepository;
    private final TaskMapper mapper;

    public ListTasksInteractor(TaskRepository taskRepository, TaskMapper mapper) {
        this.taskRepository = taskRepository;
        this.mapper = mapper;
    }

    @Override
    public PageOutput<TaskOutput> execute(ListTasksInput input, UserId owner) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
