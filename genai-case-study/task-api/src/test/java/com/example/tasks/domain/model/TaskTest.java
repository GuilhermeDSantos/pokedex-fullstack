package com.example.tasks.domain.model;

import com.example.tasks.domain.exception.InvalidTaskException;
import org.junit.jupiter.api.Test;

import static com.example.tasks.fixture.TaskFixture.ALICE;
import static com.example.tasks.fixture.TaskFixture.NOW;
import static com.example.tasks.fixture.TaskFixture.TASK_ID;
import static com.example.tasks.fixture.TaskFixture.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTest {

    @Test
    void shouldStartAsToDoForItsOwnerWhenNoStatusIsGiven() {
        var task = Task.create(TASK_ID, ALICE, new Title("Buy milk"), null, null, null, TODAY, NOW);

        assertThat(task.getOwnerId()).isEqualTo(ALICE);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCreatedAt()).isEqualTo(NOW);
        assertThat(task.getUpdatedAt()).isEqualTo(NOW);
    }

    // "Today" is the caller's, from an injected clock, so this boundary is testable.
    @Test
    void shouldAcceptADueDateOfTodayAndRejectOneInThePast() {
        assertThat(Task.create(TASK_ID, ALICE, new Title("Buy milk"), null, TaskStatus.TODO, TODAY, TODAY, NOW)
            .getDueDate()).isEqualTo(TODAY);
        assertThatThrownBy(() -> Task.create(TASK_ID, ALICE, new Title("Buy milk"), null, TaskStatus.TODO,
            TODAY.minusDays(1), TODAY, NOW))
            .isInstanceOf(InvalidTaskException.class)
            .hasMessage("Due date must not be in the past");
    }
}
