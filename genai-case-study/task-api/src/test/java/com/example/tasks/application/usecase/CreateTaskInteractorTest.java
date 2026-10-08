package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.CreateTaskInput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.domain.exception.InvalidTaskException;
import com.example.tasks.domain.model.Task;
import com.example.tasks.domain.repository.TaskRepository;
import com.example.tasks.fixture.InlineUnitOfWork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.tasks.fixture.TaskFixture.ALICE;
import static com.example.tasks.fixture.TaskFixture.NOW;
import static com.example.tasks.fixture.TaskFixture.TASK_ID;
import static com.example.tasks.fixture.TaskFixture.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateTaskInteractorTest {

    @Mock private TaskRepository taskRepository;

    private final InlineUnitOfWork unitOfWork = new InlineUnitOfWork();
    private CreateTaskInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new CreateTaskInteractor(taskRepository, new TaskMapper(), unitOfWork);
    }

    // The owner is whoever is signed in, passed in by the edge; the input can't name one.
    @Test
    void shouldSaveANewTaskForTheSignedInUser() {
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

        var output = interactor.execute(new CreateTaskInput(" Buy milk ", "", null, TODAY), ALICE, TASK_ID, TODAY, NOW);

        assertThat(output.id()).isEqualTo(TASK_ID.value());
        assertThat(output.title()).isEqualTo("Buy milk");
        assertThat(output.description()).isNull();
        assertThat(output.status()).isEqualTo("TODO");
        assertThat(output.dueDate()).isEqualTo(TODAY);
        assertThat(unitOfWork.transactions()).isEqualTo(1);
    }

    @Test
    void shouldRejectAnUnknownStatusWithoutOpeningATransaction() {
        assertThatThrownBy(() -> interactor.execute(new CreateTaskInput("Buy milk", null, "BLOCKED", null), ALICE, TASK_ID,
            TODAY, NOW))
            .isInstanceOf(InvalidTaskException.class)
            .hasMessage("Status must be one of [TODO, IN_PROGRESS, DONE]");

        assertThat(unitOfWork.transactions()).isZero();
        verifyNoInteractions(taskRepository);
    }
}
