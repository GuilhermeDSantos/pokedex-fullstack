package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.DeleteTaskInput;
import com.example.tasks.domain.model.UserId;

public interface DeleteTaskUseCase {
    void execute(DeleteTaskInput input, UserId owner);
}
