package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.GetTaskInput;
import com.example.tasks.application.dto.TaskOutput;
import com.example.tasks.domain.model.UserId;

public interface GetTaskUseCase {
    TaskOutput execute(GetTaskInput input, UserId owner);
}
