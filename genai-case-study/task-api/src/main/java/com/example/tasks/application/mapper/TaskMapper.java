package com.example.tasks.application.mapper;

import com.example.tasks.domain.exception.InvalidTaskException;
import com.example.tasks.domain.model.Description;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.TaskStatus;
import com.example.tasks.domain.model.Title;
import com.example.tasks.domain.pagination.PageRequest;

import java.util.Arrays;
import java.util.UUID;

// Raw input → domain objects. Every malformed value becomes a 400 here, before any port is called.
public class TaskMapper {

    public TaskId toTaskId(String raw) {
        if (raw == null) {
            throw new InvalidTaskException("Task id must be a UUID");
        }
        try {
            return new TaskId(UUID.fromString(raw));
        } catch (IllegalArgumentException notAUuid) {
            throw new InvalidTaskException("Task id must be a UUID");
        }
    }

    /** {@code null} stays {@code null}: the caller decides what a missing status means. */
    public TaskStatus toStatus(String raw) {
        if (raw == null) {
            return null;
        }
        return Arrays.stream(TaskStatus.values())
            .filter(status -> status.name().equals(raw))
            .findFirst()
            .orElseThrow(() -> new InvalidTaskException("Status must be one of " + Arrays.toString(TaskStatus.values())));
    }

    public Title toTitle(String raw) {
        return new Title(raw);
    }

    public Description toDescription(String raw) {
        return Description.of(raw);
    }

    public PageRequest toPageRequest(int page, int size) {
        return new PageRequest(page, size);
    }
}
