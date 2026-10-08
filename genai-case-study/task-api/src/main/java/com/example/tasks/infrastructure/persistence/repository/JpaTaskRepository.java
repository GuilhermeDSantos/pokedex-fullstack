package com.example.tasks.infrastructure.persistence.repository;

import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.TaskStatus;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.domain.pagination.Page;
import com.example.tasks.domain.pagination.PageRequest;
import com.example.tasks.domain.repository.TaskRepository;
import com.example.tasks.infrastructure.persistence.mapper.TaskEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaTaskRepository implements TaskRepository {

    private final TaskJpaRepository jpaRepository;
    private final TaskEntityMapper mapper;

    public JpaTaskRepository(TaskJpaRepository jpaRepository, TaskEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Task save(Task task) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Optional<Task> findByIdAndOwner(TaskId id, UserId owner) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Page<Task> findAllByOwner(UserId owner, TaskStatus status, PageRequest pageRequest) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void delete(Task task) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
