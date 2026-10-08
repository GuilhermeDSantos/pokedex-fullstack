package com.example.tasks.interfaces.rest;

import com.example.tasks.application.dto.CreateTaskInput;
import com.example.tasks.application.dto.DeleteTaskInput;
import com.example.tasks.application.dto.GetTaskInput;
import com.example.tasks.application.dto.ListTasksInput;
import com.example.tasks.application.dto.PageOutput;
import com.example.tasks.application.dto.TaskOutput;
import com.example.tasks.application.dto.UpdateTaskInput;
import com.example.tasks.application.usecase.CreateTaskUseCase;
import com.example.tasks.application.usecase.DeleteTaskUseCase;
import com.example.tasks.application.usecase.GetTaskUseCase;
import com.example.tasks.application.usecase.ListTasksUseCase;
import com.example.tasks.application.usecase.UpdateTaskUseCase;
import com.example.tasks.domain.exception.InvalidTaskException;
import com.example.tasks.domain.exception.TaskModifiedConcurrentlyException;
import com.example.tasks.domain.exception.TaskNotFoundException;
import com.example.tasks.infrastructure.security.SecurityConfig;
import com.example.tasks.interfaces.rest.controller.TaskController;
import com.example.tasks.interfaces.rest.security.JsonAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.example.tasks.fixture.TaskFixture.ALICE;
import static com.example.tasks.fixture.TaskFixture.NOW;
import static com.example.tasks.fixture.TaskFixture.TASK_ID;
import static com.example.tasks.fixture.TaskFixture.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

// The real security chain and error handler around the controller; the use cases are mocked.
@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, TaskControllerIT.FixedClock.class})
class TaskControllerIT {

    private static final String ID = TASK_ID.value().toString();
    private static final TaskOutput TASK = new TaskOutput(TASK_ID.value(), "Buy milk", null, "TODO", TODAY, NOW, NOW, 0);
    private static final String TASK_JSON = """
        { "id": "00000000-0000-0000-0000-00000000000a", "title": "Buy milk", "description": null, "status": "TODO",
          "dueDate": "2026-01-15", "createdAt": "2026-01-15T10:00:00Z", "updatedAt": "2026-01-15T10:00:00Z",
          "version": 0 }
        """;

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired private MockMvcTester mockMvc;

    @MockitoBean private CreateTaskUseCase createTaskUseCase;
    @MockitoBean private GetTaskUseCase getTaskUseCase;
    @MockitoBean private ListTasksUseCase listTasksUseCase;
    @MockitoBean private UpdateTaskUseCase updateTaskUseCase;
    @MockitoBean private DeleteTaskUseCase deleteTaskUseCase;
    @MockitoBean private UserDetailsService userDetailsService;

    // The principal's name is the user's id (see DatabaseUserDetailsService).
    private static org.springframework.test.web.servlet.request.RequestPostProcessor alice() {
        return user(ALICE.value().toString());
    }

    @Test
    void shouldCreateATaskForTheSignedInUser() {
        given(createTaskUseCase.execute(eq(new CreateTaskInput("Buy milk", null, null, TODAY)), eq(ALICE), any(),
            eq(TODAY), eq(NOW))).willReturn(TASK);

        assertThat(mockMvc.post().uri("/api/v1/tasks").with(alice()).contentType(APPLICATION_JSON)
            .content("{ \"title\": \"Buy milk\", \"dueDate\": \"2026-01-15\" }"))
            .hasStatus(201)
            .hasHeader("Location", "http://localhost/api/v1/tasks/" + ID)
            .bodyJson().isStrictlyEqualTo(TASK_JSON);
    }

    @Test
    void shouldReturnOneTask() {
        given(getTaskUseCase.execute(new GetTaskInput(ID), ALICE)).willReturn(TASK);

        assertThat(mockMvc.get().uri("/api/v1/tasks/" + ID).with(alice())).hasStatusOk().bodyJson()
            .isStrictlyEqualTo(TASK_JSON);
    }

    @Test
    void shouldListTheUsersTasksByStatusAPageAtATime() {
        given(listTasksUseCase.execute(new ListTasksInput("TODO", 1, 10), ALICE))
            .willReturn(new PageOutput<>(List.of(TASK), 1, 10, 11));

        assertThat(mockMvc.get().uri("/api/v1/tasks?status=TODO&page=1&size=10").with(alice())).hasStatusOk()
            .bodyJson().isLenientlyEqualTo("{ \"page\": 1, \"size\": 10, \"totalElements\": 11, \"totalPages\": 2 }");
    }

    @Test
    void shouldUpdateATask() {
        given(updateTaskUseCase.execute(new UpdateTaskInput(ID, "Buy milk", null, "DONE", null, 0), ALICE, TODAY, NOW))
            .willReturn(TASK);

        assertThat(mockMvc.put().uri("/api/v1/tasks/" + ID).with(alice()).contentType(APPLICATION_JSON)
            .content("{ \"title\": \"Buy milk\", \"status\": \"DONE\", \"version\": 0 }")).hasStatusOk();
    }

    @Test
    void shouldDeleteATask() {
        assertThat(mockMvc.delete().uri("/api/v1/tasks/" + ID).with(alice())).hasStatus(204);

        then(deleteTaskUseCase).should().execute(new DeleteTaskInput(ID), ALICE);
    }

    @Test
    void shouldRequireCredentials() {
        assertThat(mockMvc.get().uri("/api/v1/tasks")).hasStatus(401)
            .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
        verifyNoInteractions(listTasksUseCase);
    }

    @Test
    void shouldAnswerAMalformedBodyWith400WithoutEchoingIt() {
        assertThat(mockMvc.post().uri("/api/v1/tasks").with(alice()).contentType(APPLICATION_JSON)
            .content("{ \"title\": \"Buy milk\", \"dueDate\": \"not a date\" }"))
            .hasStatus(400)
            .bodyJson().extractingPath("$.message").isEqualTo("Malformed JSON request body");
        verifyNoInteractions(createTaskUseCase);
    }

    @Test
    void shouldAnswerATitleOverTheLimitWith400NamingTheField() {
        assertThat(mockMvc.post().uri("/api/v1/tasks").with(alice()).contentType(APPLICATION_JSON)
            .content("{ \"title\": \"" + "a".repeat(201) + "\" }"))
            .hasStatus(400)
            .bodyJson().extractingPath("$.fieldErrors[0].field").isEqualTo("title");
    }

    @Test
    void shouldAnswerAnUpdateWithoutAVersionWith400() {
        assertThat(mockMvc.put().uri("/api/v1/tasks/" + ID).with(alice()).contentType(APPLICATION_JSON)
            .content("{ \"title\": \"Buy milk\", \"status\": \"DONE\" }"))
            .hasStatus(400)
            .bodyJson().extractingPath("$.fieldErrors[0].field").isEqualTo("version");
    }

    @Test
    void shouldAnswerAnInvalidValueWith400() {
        given(createTaskUseCase.execute(any(), any(), any(), any(), any()))
            .willThrow(new InvalidTaskException("Due date must not be in the past"));

        assertThat(mockMvc.post().uri("/api/v1/tasks").with(alice()).contentType(APPLICATION_JSON)
            .content("{ \"title\": \"Buy milk\" }"))
            .hasStatus(400)
            .bodyJson().extractingPath("$.message").isEqualTo("Due date must not be in the past");
    }

    @Test
    void shouldAnswerAMissingOrForeignTaskWith404() {
        given(getTaskUseCase.execute(new GetTaskInput(ID), ALICE)).willThrow(new TaskNotFoundException(TASK_ID));

        assertThat(mockMvc.get().uri("/api/v1/tasks/" + ID).with(alice())).hasStatus(404);
    }

    @Test
    void shouldAnswerAStaleUpdateWith409() {
        given(updateTaskUseCase.execute(any(), any(), any(), any())).willThrow(new TaskModifiedConcurrentlyException());

        assertThat(mockMvc.put().uri("/api/v1/tasks/" + ID).with(alice()).contentType(APPLICATION_JSON)
            .content("{ \"title\": \"Buy milk\", \"status\": \"DONE\", \"version\": 0 }")).hasStatus(409);
    }

    @Test
    void shouldAnswerABadPageParameterWith400() {
        assertThat(mockMvc.get().uri("/api/v1/tasks?size=lots").with(alice())).hasStatus(400);
    }
}
