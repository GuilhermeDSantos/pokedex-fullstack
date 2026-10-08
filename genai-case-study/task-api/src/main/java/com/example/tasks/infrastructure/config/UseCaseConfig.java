package com.example.tasks.infrastructure.config;

import com.example.tasks.application.mapper.TaskMapper;
import com.example.tasks.application.port.UnitOfWork;
import com.example.tasks.application.usecase.CreateTaskInteractor;
import com.example.tasks.application.usecase.CreateTaskUseCase;
import com.example.tasks.application.usecase.DeleteTaskInteractor;
import com.example.tasks.application.usecase.DeleteTaskUseCase;
import com.example.tasks.application.usecase.GetTaskInteractor;
import com.example.tasks.application.usecase.GetTaskUseCase;
import com.example.tasks.application.usecase.ListTasksInteractor;
import com.example.tasks.application.usecase.ListTasksUseCase;
import com.example.tasks.application.usecase.UpdateTaskInteractor;
import com.example.tasks.application.usecase.UpdateTaskUseCase;
import com.example.tasks.domain.repository.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// The composition root: the only place that knows the interactors. Each bean is typed as its input port.
@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TaskMapper taskMapper() {
        return new TaskMapper();
    }

    @Bean
    CreateTaskUseCase createTaskUseCase(TaskRepository taskRepository, TaskMapper mapper, UnitOfWork unitOfWork) {
        return new CreateTaskInteractor(taskRepository, mapper, unitOfWork);
    }

    @Bean
    GetTaskUseCase getTaskUseCase(TaskRepository taskRepository, TaskMapper mapper) {
        return new GetTaskInteractor(taskRepository, mapper);
    }

    @Bean
    ListTasksUseCase listTasksUseCase(TaskRepository taskRepository, TaskMapper mapper) {
        return new ListTasksInteractor(taskRepository, mapper);
    }

    @Bean
    UpdateTaskUseCase updateTaskUseCase(TaskRepository taskRepository, TaskMapper mapper, UnitOfWork unitOfWork) {
        return new UpdateTaskInteractor(taskRepository, mapper, unitOfWork);
    }

    @Bean
    DeleteTaskUseCase deleteTaskUseCase(TaskRepository taskRepository, TaskMapper mapper, UnitOfWork unitOfWork) {
        return new DeleteTaskInteractor(taskRepository, mapper, unitOfWork);
    }
}
