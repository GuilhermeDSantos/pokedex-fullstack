package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.ListTasksInput;
import com.example.tasks.application.dto.PageOutput;
import com.example.tasks.application.dto.TaskOutput;
import com.example.tasks.domain.model.UserId;

public interface ListTasksUseCase {
    PageOutput<TaskOutput> execute(ListTasksInput input, UserId owner);
}
