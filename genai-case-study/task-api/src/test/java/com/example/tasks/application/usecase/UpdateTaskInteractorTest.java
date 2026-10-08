package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.UpdateTaskInput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.domain.exception.TaskModifiedConcurrentlyException;
import com.example.tasks.domain.exception.TaskNotFoundException;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.repository.TaskRepository;
import com.example.tasks.fixture.InlineUnitOfWork;
import com.example.tasks.fixture.TaskFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.tasks.fixture.TaskFixture.ALICE;
import static com.example.tasks.fixture.TaskFixture.BOB;
import static com.example.tasks.fixture.TaskFixture.NOW;
import static com.example.tasks.fixture.TaskFixture.TASK_ID;
import static com.example.tasks.fixture.TaskFixture.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UpdateTaskInteractorTest {

    private static final String ID = TASK_ID.value().toString();

    @Mock private TaskRepository taskRepository;

    private UpdateTaskInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new UpdateTaskInteractor(taskRepository, new TaskMapper(), new InlineUnitOfWork());
    }

    @Test
    void shouldReplaceTheOwnersTaskAndSaveIt() {
        given(taskRepository.getByIdAndOwner(TASK_ID, ALICE)).willReturn(TaskFixture.alicesTask());
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

        var output = interactor.execute(new UpdateTaskInput(ID, "Buy oat milk", null, "DONE", null, 3), ALICE, TODAY,
            NOW.plusSeconds(60));

        assertThat(output.title()).isEqualTo("Buy oat milk");
        assertThat(output.status()).isEqualTo("DONE");
        assertThat(output.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void shouldNotFindAnotherUsersTask() {
        given(taskRepository.getByIdAndOwner(TASK_ID, BOB)).willThrow(new TaskNotFoundException(TASK_ID));

        assertThatThrownBy(() -> interactor.execute(new UpdateTaskInput(ID, "Mine now", null, "DONE", null, 3), BOB,
            TODAY, NOW))
            .isInstanceOf(TaskNotFoundException.class);

        then(taskRepository).should(never()).save(any());
    }

    @Test
    void shouldRefuseAStaleVersionWithoutSaving() {
        given(taskRepository.getByIdAndOwner(TASK_ID, ALICE)).willReturn(TaskFixture.alicesTask());

        assertThatThrownBy(() -> interactor.execute(new UpdateTaskInput(ID, "Buy milk", null, "DONE", null, 2), ALICE,
            TODAY, NOW))
            .isInstanceOf(TaskModifiedConcurrentlyException.class);

        then(taskRepository).should(never()).save(any());
    }
}
