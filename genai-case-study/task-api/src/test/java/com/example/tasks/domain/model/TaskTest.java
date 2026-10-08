package com.example.tasks.domain.model;

import com.example.tasks.domain.exception.InvalidTaskException;
import com.example.tasks.domain.exception.TaskModifiedConcurrentlyException;
import com.example.tasks.fixture.TaskFixture;
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

    @Test
    void shouldReplaceTheFourFieldsOnUpdate() {
        var task = TaskFixture.alicesTask();
        var later = NOW.plusSeconds(60);

        task.update(new Title("Buy oat milk"), null, TaskStatus.DONE, null, 3, TODAY, later);

        assertThat(task.getTitle()).isEqualTo(new Title("Buy oat milk"));
        assertThat(task.getDescription()).isNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getDueDate()).isNull();
        assertThat(task.getUpdatedAt()).isEqualTo(later);
        assertThat(task.getCreatedAt()).isEqualTo(NOW);
    }

    // A task that became overdue can still be edited; only a new date is checked against today.
    @Test
    void shouldKeepAnUnchangedDueDateEvenWhenItIsNowInThePast() {
        var task = TaskFixture.alicesTask();
        var aWeekLater = TODAY.plusDays(7);

        task.update(new Title("Buy milk"), null, TaskStatus.IN_PROGRESS, TODAY.plusDays(1), 3, aWeekLater, NOW);

        assertThat(task.getDueDate()).isEqualTo(TODAY.plusDays(1));
        assertThatThrownBy(() -> task.update(new Title("Buy milk"), null, TaskStatus.IN_PROGRESS, TODAY.plusDays(2), 3,
            aWeekLater, NOW))
            .isInstanceOf(InvalidTaskException.class)
            .hasMessage("Due date must not be in the past");
    }

    @Test
    void shouldRequireAStatusOnUpdate() {
        assertThatThrownBy(() -> TaskFixture.alicesTask().update(new Title("Buy milk"), null, null, null, 3, TODAY, NOW))
            .isInstanceOf(InvalidTaskException.class)
            .hasMessage("Status is required");
    }

    // The client sends the version it read: an older one means someone else saved in between.
    @Test
    void shouldRefuseAnUpdateBasedOnAStaleVersion() {
        assertThatThrownBy(() -> TaskFixture.alicesTask().update(new Title("Buy milk"), null, TaskStatus.DONE, null, 2,
            TODAY, NOW))
            .isInstanceOf(TaskModifiedConcurrentlyException.class);
    }
}
