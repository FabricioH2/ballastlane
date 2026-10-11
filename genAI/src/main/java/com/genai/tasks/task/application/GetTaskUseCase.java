package com.genai.tasks.task.application;

import com.genai.tasks.task.application.port.out.TaskRepository;
import com.genai.tasks.task.domain.Task;

import java.util.UUID;

public class GetTaskUseCase {

    private final TaskRepository tasks;

    public GetTaskUseCase(TaskRepository tasks) {
        this.tasks = tasks;
    }

    public Task execute(UUID ownerId, UUID taskId) {
        return tasks.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
