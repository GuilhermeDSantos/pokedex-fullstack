package com.example.tasks.domain.exception;

public class TaskModifiedConcurrentlyException extends ConflictException {

    public TaskModifiedConcurrentlyException() {
        super("The task was changed in the meantime. Reload it and try again");
    }
}
