package com.genai.tasks.task.application;

import com.genai.tasks.task.application.port.out.TaskRepository;
import com.genai.tasks.task.domain.Task;

import java.time.Clock;

public class CreateTaskUseCase {

    private final TaskRepository tasks;
    private final Clock clock;

    public CreateTaskUseCase(TaskRepository tasks, Clock clock) {
        this.tasks = tasks;
        this.clock = clock;
    }

    public Task execute(CreateTaskCommand command) {
        Task task = Task.create(command.ownerId(), command.title(), command.description(), command.status(),
                command.dueDate(), clock);
        return tasks.save(task);
    }
}
