package com.example.tasks.infrastructure.persistence;

import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.TaskStatus;
import com.example.tasks.domain.model.Title;
import com.example.tasks.domain.pagination.PageRequest;
import com.example.tasks.fixture.TaskFixture;
import com.example.tasks.infrastructure.persistence.mapper.TaskEntityMapper;
import com.example.tasks.infrastructure.persistence.repository.JpaTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static com.example.tasks.fixture.TaskFixture.ALICE;
import static com.example.tasks.fixture.TaskFixture.BOB;
import static com.example.tasks.fixture.TaskFixture.NOW;
import static com.example.tasks.fixture.TaskFixture.TASK_ID;
import static com.example.tasks.fixture.TaskFixture.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

// Real PostgreSQL with the real migrations; ALICE and BOB are the seeded users.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaTaskRepository.class, TaskEntityMapper.class})
@Testcontainers
class JpaTaskRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JpaTaskRepository repository;

    @Test
    void shouldSaveAndReloadTheWholeTaskForItsOwnerOnly() {
        var saved = repository.save(Task.create(TASK_ID, ALICE, new Title("Buy milk"), null, null, TODAY, TODAY, NOW));

        assertThat(repository.findByIdAndOwner(TASK_ID, ALICE)).hasValueSatisfying(reloaded -> {
            assertThat(reloaded.getTitle()).isEqualTo(new Title("Buy milk"));
            assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(reloaded.getDueDate()).isEqualTo(TODAY);
            assertThat(reloaded.getVersion()).isEqualTo(saved.getVersion());
        });
        assertThat(repository.findByIdAndOwner(TASK_ID, BOB)).isEmpty();
    }

    @Test
    void shouldSaveEveryEditAndCountTheVersion() {
        repository.save(Task.create(TASK_ID, ALICE, new Title("Buy milk"), null, null, null, TODAY, NOW));

        edit("Buy oat milk", TaskStatus.IN_PROGRESS);
        edit("Buy oat milk", TaskStatus.DONE);

        assertThat(repository.findByIdAndOwner(TASK_ID, ALICE)).hasValueSatisfying(reloaded -> {
            assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(reloaded.getVersion()).isEqualTo(2);
        });
    }

    @Test
    void shouldListOnlyTheOwnersTasksByStatusAPageAtATime() {
        for (int i = 1; i <= 3; i++) {
            repository.save(Task.create(new TaskId(UUID.randomUUID()), ALICE, new Title("Todo " + i), null, null, null,
                TODAY, NOW.plusSeconds(i)));
        }
        repository.save(Task.create(new TaskId(UUID.randomUUID()), ALICE, new Title("Done"), null, TaskStatus.DONE, null,
            TODAY, NOW));
        repository.save(Task.create(new TaskId(UUID.randomUUID()), BOB, new Title("Bob's"), null, null, null, TODAY, NOW));

        var page = repository.findAllByOwner(ALICE, TaskStatus.TODO, new PageRequest(0, 2));

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.content()).extracting(task -> task.getTitle().value()).containsExactly("Todo 3", "Todo 2");
        assertThat(repository.findAllByOwner(ALICE, null, new PageRequest(0, 20)).totalElements()).isEqualTo(4);
    }

    @Test
    void shouldDeleteTheTask() {
        var task = repository.save(Task.create(TASK_ID, ALICE, new Title("Buy milk"), null, null, null, TODAY, NOW));

        repository.delete(task);

        assertThat(repository.findByIdAndOwner(TASK_ID, ALICE)).isEmpty();
    }

    private void edit(String title, TaskStatus status) {
        var task = repository.getByIdAndOwner(TASK_ID, ALICE);
        task.update(new Title(title), null, status, null, task.getVersion(), TODAY, NOW);
        repository.save(task);
    }
}
