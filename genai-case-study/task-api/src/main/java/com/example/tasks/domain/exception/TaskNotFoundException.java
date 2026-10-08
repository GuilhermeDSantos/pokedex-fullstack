package com.example.tasks.domain.exception;

import com.example.tasks.domain.model.TaskId;

// Also what another user's task answers: the two must be indistinguishable, so ids can't be probed.
public class TaskNotFoundException extends NotFoundException {

    public TaskNotFoundException(TaskId id) {
        super("Task " + id.value() + " was not found");
    }
}
