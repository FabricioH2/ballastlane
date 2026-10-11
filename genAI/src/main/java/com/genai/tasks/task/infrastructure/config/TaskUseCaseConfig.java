package com.genai.tasks.task.infrastructure.config;

import com.genai.tasks.task.application.CreateTaskUseCase;
import com.genai.tasks.task.application.DeleteTaskUseCase;
import com.genai.tasks.task.application.GetTaskUseCase;
import com.genai.tasks.task.application.ListTasksUseCase;
import com.genai.tasks.task.application.UpdateTaskUseCase;
import com.genai.tasks.task.application.port.out.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the framework-free use cases into the Spring context.
 */
@Configuration(proxyBeanMethods = false)
class TaskUseCaseConfig {

    @Bean
    CreateTaskUseCase createTaskUseCase(TaskRepository tasks, Clock clock) {
        return new CreateTaskUseCase(tasks, clock);
    }

    @Bean
    GetTaskUseCase getTaskUseCase(TaskRepository tasks) {
        return new GetTaskUseCase(tasks);
    }

    @Bean
    ListTasksUseCase listTasksUseCase(TaskRepository tasks) {
        return new ListTasksUseCase(tasks);
    }

    @Bean
    UpdateTaskUseCase updateTaskUseCase(TaskRepository tasks, Clock clock) {
        return new UpdateTaskUseCase(tasks, clock);
    }

    @Bean
    DeleteTaskUseCase deleteTaskUseCase(TaskRepository tasks) {
        return new DeleteTaskUseCase(tasks);
    }
}
