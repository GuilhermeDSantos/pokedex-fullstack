package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.DeleteTaskInput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.domain.exception.TaskNotFoundException;
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
import static com.example.tasks.fixture.TaskFixture.TASK_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DeleteTaskInteractorTest {

    private static final String ID = TASK_ID.value().toString();

    @Mock private TaskRepository taskRepository;

    private DeleteTaskInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new DeleteTaskInteractor(taskRepository, new TaskMapper(), new InlineUnitOfWork());
    }

    @Test
    void shouldDeleteTheOwnersTask() {
        var task = TaskFixture.alicesTask();
        given(taskRepository.getByIdAndOwner(TASK_ID, ALICE)).willReturn(task);

        interactor.execute(new DeleteTaskInput(ID), ALICE);

        then(taskRepository).should().delete(task);
    }

    // Deleting what isn't yours, or isn't there, must not answer 204 as if it worked.
    @Test
    void shouldNotFindAnotherUsersTaskToDelete() {
        given(taskRepository.getByIdAndOwner(TASK_ID, BOB)).willThrow(new TaskNotFoundException(TASK_ID));

        assertThatThrownBy(() -> interactor.execute(new DeleteTaskInput(ID), BOB))
            .isInstanceOf(TaskNotFoundException.class);

        then(taskRepository).should(never()).delete(any());
    }
}
