package com.example.tasks.infrastructure.persistence.mapper;

import com.example.tasks.domain.model.Description;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.Title;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.infrastructure.persistence.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskEntityMapper {

    public Task toDomain(TaskEntity entity) {
        return Task.builder()
            .id(new TaskId(entity.getId()))
            .ownerId(new UserId(entity.getOwnerId()))
            .title(new Title(entity.getTitle()))
            .description(Description.of(entity.getDescription()))
            .status(entity.getStatus())
            .dueDate(entity.getDueDate())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .version(entity.getVersion())
            .build();
    }

    public TaskEntity toEntity(Task task) {
        var entity = new TaskEntity();
        entity.setId(task.getId().value());
        entity.setOwnerId(task.getOwnerId().value());
        return copyInto(task, entity);
    }

    // The version is not copied: Hibernate owns it on the entity it loaded.
    public TaskEntity copyInto(Task task, TaskEntity entity) {
        entity.setTitle(task.getTitle().value());
        entity.setDescription(task.getDescription() == null ? null : task.getDescription().value());
        entity.setStatus(task.getStatus());
        entity.setDueDate(task.getDueDate());
        entity.setCreatedAt(task.getCreatedAt());
        entity.setUpdatedAt(task.getUpdatedAt());
        return entity;
    }
}
