package com.example.tasks.domain.repository;

import com.example.tasks.domain.exception.NotFoundException;
import com.example.tasks.domain.exception.TaskNotFoundException;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.TaskStatus;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.domain.pagination.Page;
import com.example.tasks.domain.pagination.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.example.tasks.fixture.TaskFixture.ALICE;
import static com.example.tasks.fixture.TaskFixture.TASK_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskRepositoryTest {

    // A repository with no tasks at all: enough to exercise the port's own default method.
    private final TaskRepository emptyRepository = new TaskRepository() {
        @Override
        public Task save(Task task) {
            throw new UnsupportedOperationException("not used here");
        }

        @Override
        public Optional<Task> findByIdAndOwner(TaskId id, UserId owner) {
            return Optional.empty();
        }

        @Override
        public Page<Task> findAllByOwner(UserId owner, TaskStatus status, PageRequest pageRequest) {
            throw new UnsupportedOperationException("not used here");
        }

        @Override
        public void delete(Task task) {
            throw new UnsupportedOperationException("not used here");
        }
    };

    @Test
    void shouldTurnATaskTheOwnerDoesNotHaveIntoANotFoundError() {
        assertThatThrownBy(() -> emptyRepository.getByIdAndOwner(TASK_ID, ALICE))
            .isInstanceOf(TaskNotFoundException.class)
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Task 00000000-0000-0000-0000-00000000000a was not found");
    }
}
