package com.example.tasks.application.usecase;

import com.example.tasks.application.dto.ListTasksInput;
import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.domain.exception.InvalidPageRequestException;
import com.example.tasks.domain.model.TaskStatus;
import com.example.tasks.domain.pagination.Page;
import com.example.tasks.domain.pagination.PageRequest;
import com.example.tasks.domain.repository.TaskRepository;
import com.example.tasks.fixture.TaskFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.example.tasks.fixture.TaskFixture.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ListTasksInteractorTest {

    @Mock private TaskRepository taskRepository;

    private ListTasksInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new ListTasksInteractor(taskRepository, new TaskMapper());
    }

    @Test
    void shouldListTheCallersTasksWithTheStatusFilter() {
        given(taskRepository.findAllByOwner(ALICE, TaskStatus.TODO, new PageRequest(1, 20)))
            .willReturn(new Page<>(List.of(TaskFixture.alicesTask()), 21));

        var output = interactor.execute(new ListTasksInput("TODO", 1, 20), ALICE);

        assertThat(output.content()).singleElement().satisfies(task -> assertThat(task.title()).isEqualTo("Buy milk"));
        assertThat(output.page()).isEqualTo(1);
        assertThat(output.totalElements()).isEqualTo(21);
        assertThat(output.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldRejectAnOversizedPageBeforeAskingTheRepository() {
        assertThatThrownBy(() -> interactor.execute(new ListTasksInput(null, 0, 101), ALICE))
            .isInstanceOf(InvalidPageRequestException.class);

        verifyNoInteractions(taskRepository);
    }
}
