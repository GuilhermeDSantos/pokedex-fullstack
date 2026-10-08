package com.example.tasks.domain.repository;

import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.TaskStatus;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.domain.pagination.Page;
import com.example.tasks.domain.pagination.PageRequest;

import java.util.Optional;

// Every lookup is scoped to an owner: there is no way to read a task without saying whose it is.
public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findByIdAndOwner(TaskId id, UserId owner);

    default Task getByIdAndOwner(TaskId id, UserId owner) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** {@code status} is optional: {@code null} lists every status. */
    Page<Task> findAllByOwner(UserId owner, TaskStatus status, PageRequest pageRequest);

    void delete(Task task);
}
