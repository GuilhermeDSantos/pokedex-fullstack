package com.example.tasks.infrastructure.persistence.repository;

import com.example.tasks.domain.model.TaskStatus;
import com.example.tasks.infrastructure.persistence.entity.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {

    Optional<TaskEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<TaskEntity> findAllByOwnerId(UUID ownerId, Pageable pageable);

    Page<TaskEntity> findAllByOwnerIdAndStatus(UUID ownerId, TaskStatus status, Pageable pageable);
}
