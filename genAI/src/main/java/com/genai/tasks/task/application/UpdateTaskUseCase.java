package com.genai.tasks.task.application;

import com.genai.tasks.task.application.port.out.TaskRepository;
import com.genai.tasks.task.domain.Task;

import java.time.Clock;

public class UpdateTaskUseCase {

    private final TaskRepository tasks;
    private final Clock clock;

    public UpdateTaskUseCase(TaskRepository tasks, Clock clock) {
        this.tasks = tasks;
        this.clock = clock;
    }

    public Task execute(UpdateTaskCommand command) {
        Task task = tasks.findByIdAndOwnerId(command.taskId(), command.ownerId())
                .orElseThrow(() -> new TaskNotFoundException(command.taskId()));
        task.update(command.title(), command.description(), command.status(), command.dueDate(), clock);
        return tasks.save(task);
    }
}
