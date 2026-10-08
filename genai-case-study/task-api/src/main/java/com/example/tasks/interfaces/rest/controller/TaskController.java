package com.example.tasks.interfaces.rest.controller;

import com.example.tasks.application.dto.CreateTaskInput;
import com.example.tasks.application.dto.DeleteTaskInput;
import com.example.tasks.application.dto.GetTaskInput;
import com.example.tasks.application.dto.ListTasksInput;
import com.example.tasks.application.dto.UpdateTaskInput;
import com.example.tasks.application.usecase.CreateTaskUseCase;
import com.example.tasks.application.usecase.DeleteTaskUseCase;
import com.example.tasks.application.usecase.GetTaskUseCase;
import com.example.tasks.application.usecase.ListTasksUseCase;
import com.example.tasks.application.usecase.UpdateTaskUseCase;
import com.example.tasks.domain.model.TaskId;
import com.example.tasks.domain.model.UserId;
import com.example.tasks.interfaces.rest.request.CreateTaskRequest;
import com.example.tasks.interfaces.rest.request.UpdateTaskRequest;
import com.example.tasks.interfaces.rest.response.PageResponse;
import com.example.tasks.interfaces.rest.response.TaskResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// The edge: the only place that reads the clock, mints ids and knows who is signed in.
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final CreateTaskUseCase createTaskUseCase;
    private final GetTaskUseCase getTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final Clock clock;

    public TaskController(CreateTaskUseCase createTaskUseCase, GetTaskUseCase getTaskUseCase,
                          ListTasksUseCase listTasksUseCase, UpdateTaskUseCase updateTaskUseCase,
                          DeleteTaskUseCase deleteTaskUseCase, Clock clock) {
        this.createTaskUseCase = createTaskUseCase;
        this.getTaskUseCase = getTaskUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@RequestBody @Valid CreateTaskRequest request, Principal principal) {
        var input = new CreateTaskInput(request.title(), request.description(), request.status(), request.dueDate());
        var task = createTaskUseCase.execute(input, owner(principal), TaskId.generate(), LocalDate.now(clock),
            Instant.now(clock));
        var location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(task.id()).toUri();
        return ResponseEntity.created(location).body(TaskResponse.from(task));
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable String id, Principal principal) {
        return TaskResponse.from(getTaskUseCase.execute(new GetTaskInput(id), owner(principal)));
    }

    @GetMapping
    public PageResponse<TaskResponse> list(@RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size, Principal principal) {
        var tasks = listTasksUseCase.execute(new ListTasksInput(status, page, size), owner(principal));
        return new PageResponse<>(tasks.content().stream().map(TaskResponse::from).toList(), tasks.page(),
            tasks.size(), tasks.totalElements(), tasks.totalPages());
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable String id, @RequestBody @Valid UpdateTaskRequest request,
                               Principal principal) {
        var input = new UpdateTaskInput(id, request.title(), request.description(), request.status(), request.dueDate(),
            request.version());
        return TaskResponse.from(updateTaskUseCase.execute(input, owner(principal), LocalDate.now(clock),
            Instant.now(clock)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, Principal principal) {
        deleteTaskUseCase.execute(new DeleteTaskInput(id), owner(principal));
    }

    // The principal's name is the user's id, set by the user lookup at sign-in.
    private static UserId owner(Principal principal) {
        return new UserId(UUID.fromString(principal.getName()));
    }
}
