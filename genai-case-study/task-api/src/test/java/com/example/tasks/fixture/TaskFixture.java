package com.example.tasks.fixture;

import com.example.tasks.domain.model.Description;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.TaskStatus;
import com.example.tasks.domain.model.Title;
import com.example.tasks.domain.model.UserId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class TaskFixture {

    public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    public static final LocalDate TODAY = LocalDate.parse("2026-01-15");
    public static final TaskId TASK_ID = new TaskId(UUID.fromString("00000000-0000-0000-0000-00000000000a"));
    public static final UserId ALICE = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    public static final UserId BOB = new UserId(UUID.fromString("00000000-0000-0000-0000-0000000000b0"));

    private TaskFixture() {
    }

    // Alice's task as it is stored: version 3, due tomorrow.
    public static Task alicesTask() {
        return Task.builder()
            .id(TASK_ID)
            .ownerId(ALICE)
            .title(new Title("Buy milk"))
            .description(Description.of("Two litres"))
            .status(TaskStatus.TODO)
            .dueDate(TODAY.plusDays(1))
            .createdAt(NOW)
            .updatedAt(NOW)
            .version(3)
            .build();
    }
}
