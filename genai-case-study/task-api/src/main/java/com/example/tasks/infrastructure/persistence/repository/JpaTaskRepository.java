package com.example.tasks.infrastructure.persistence.repository;

import com.example.tasks.domain.exception.TaskModifiedConcurrentlyException;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.TaskStatus;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.domain.pagination.Page;
import com.example.tasks.domain.pagination.PageRequest;
import com.example.tasks.domain.repository.TaskRepository;
import com.example.tasks.infrastructure.persistence.mapper.TaskEntityMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
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
        try {
            // An edit updates the managed entity, so Hibernate checks the version it read on flush.
            var entity = jpaRepository.findById(task.getId().value())
                .map(existing -> mapper.copyInto(task, existing))
                .orElseGet(() -> mapper.toEntity(task));
            return mapper.toDomain(jpaRepository.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException exception) {
            throw new TaskModifiedConcurrentlyException();
        }
    }

    @Override
    public Optional<Task> findByIdAndOwner(TaskId id, UserId owner) {
        return jpaRepository.findByIdAndOwnerId(id.value(), owner.value()).map(mapper::toDomain);
    }

    @Override
    public Page<Task> findAllByOwner(UserId owner, TaskStatus status, PageRequest pageRequest) {
        // Newest first; the id breaks ties so a page never repeats or skips a task.
        var pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size(),
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        var page = status == null
            ? jpaRepository.findAllByOwnerId(owner.value(), pageable)
            : jpaRepository.findAllByOwnerIdAndStatus(owner.value(), status, pageable);
        return new Page<>(page.getContent().stream().map(mapper::toDomain).toList(), page.getTotalElements());
    }

    @Override
    public void delete(Task task) {
        jpaRepository.deleteById(task.getId().value());
        jpaRepository.flush();
    }
}
