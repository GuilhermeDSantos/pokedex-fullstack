package com.example.tasks.domain.model;

import com.example.tasks.domain.exception.InvalidTaskException;
import com.example.tasks.domain.exception.TaskModifiedConcurrentlyException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A user's task. New tasks come from {@link #create}; {@link #builder()} only rebuilds one that was
 * already valid when it was stored (persistence mapper).
 */
public class Task {

    private final TaskId id;
    private final UserId ownerId;
    private Title title;
    private Description description;
    private TaskStatus status;
    private LocalDate dueDate;
    private final Instant createdAt;
    private Instant updatedAt;
    private final long version;

    private Task(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id must not be null");
        this.ownerId = Objects.requireNonNull(builder.ownerId, "ownerId must not be null");
        this.title = Objects.requireNonNull(builder.title, "title must not be null");
        this.description = builder.description;
        this.status = Objects.requireNonNull(builder.status, "status must not be null");
        this.dueDate = builder.dueDate;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "updatedAt must not be null");
        this.version = builder.version;
    }

    public static Task create(TaskId id, UserId ownerId, Title title, Description description, TaskStatus status,
                              LocalDate dueDate, LocalDate today, Instant now) {
        requireNotInThePast(dueDate, today);
        return builder()
            .id(id)
            .ownerId(ownerId)
            .title(title)
            .description(description)
            .status(status == null ? TaskStatus.TODO : status)
            .dueDate(dueDate)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    // PUT semantics: the four fields are replaced; the caller's version must be the stored one.
    public void update(Title title, Description description, TaskStatus status, LocalDate dueDate,
                       long expectedVersion, LocalDate today, Instant now) {
        if (expectedVersion != version) {
            throw new TaskModifiedConcurrentlyException();
        }
        if (status == null) {
            throw new InvalidTaskException("Status is required");
        }
        if (!Objects.equals(dueDate, this.dueDate)) {
            requireNotInThePast(dueDate, today);
        }
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = description;
        this.status = status;
        this.dueDate = dueDate;
        this.updatedAt = now;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void requireNotInThePast(LocalDate dueDate, LocalDate today) {
        if (dueDate != null && dueDate.isBefore(today)) {
            throw new InvalidTaskException("Due date must not be in the past");
        }
    }

    public TaskId getId() { return id; }
    public UserId getOwnerId() { return ownerId; }
    public Title getTitle() { return title; }
    public Description getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Task other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static final class Builder {

        private TaskId id;
        private UserId ownerId;
        private Title title;
        private Description description;
        private TaskStatus status;
        private LocalDate dueDate;
        private Instant createdAt;
        private Instant updatedAt;
        private long version;

        private Builder() {
        }

        public Builder id(TaskId id) { this.id = id; return this; }
        public Builder ownerId(UserId ownerId) { this.ownerId = ownerId; return this; }
        public Builder title(Title title) { this.title = title; return this; }
        public Builder description(Description description) { this.description = description; return this; }
        public Builder status(TaskStatus status) { this.status = status; return this; }
        public Builder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder version(long version) { this.version = version; return this; }

        public Task build() {
            return new Task(this);
        }
    }
}
