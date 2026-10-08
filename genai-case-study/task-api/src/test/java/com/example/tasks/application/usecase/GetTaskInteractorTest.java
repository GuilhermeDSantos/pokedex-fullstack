package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.GetTaskInput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.domain.exception.InvalidTaskException;
import com.example.tasks.domain.exception.TaskNotFoundException;
import com.example.tasks.domain.repository.TaskRepository;
import com.example.tasks.fixture.TaskFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.tasks.fixture.TaskFixture.ALICE;
import static com.example.tasks.fixture.TaskFixture.BOB;
import static com.example.tasks.fixture.TaskFixture.TASK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetTaskInteractorTest {

    @Mock private TaskRepository taskRepository;

    private GetTaskInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new GetTaskInteractor(taskRepository, new TaskMapper());
    }

    @Test
    void shouldReturnTheOwnersTask() {
        given(taskRepository.getByIdAndOwner(TASK_ID, ALICE)).willReturn(TaskFixture.alicesTask());

        assertThat(interactor.execute(new GetTaskInput(TASK_ID.value().toString()), ALICE).title()).isEqualTo("Buy milk");
    }

    // The lookup is scoped to the caller, so another user's task is simply not there.
    @Test
    void shouldNotFindAnotherUsersTask() {
        given(taskRepository.getByIdAndOwner(TASK_ID, BOB)).willThrow(new TaskNotFoundException(TASK_ID));

        assertThatThrownBy(() -> interactor.execute(new GetTaskInput(TASK_ID.value().toString()), BOB))
            .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldRejectAnIdThatIsNotAUuid() {
        assertThatThrownBy(() -> interactor.execute(new GetTaskInput("42"), ALICE))
            .isInstanceOf(InvalidTaskException.class)
            .hasMessage("Task id must be a UUID");

        verifyNoInteractions(taskRepository);
    }
}
