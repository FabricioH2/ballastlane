package com.genai.tasks.task.application;

import com.genai.tasks.task.application.port.out.TaskRepository;

import java.util.UUID;

public class DeleteTaskUseCase {

    private final TaskRepository tasks;

    public DeleteTaskUseCase(TaskRepository tasks) {
        this.tasks = tasks;
    }

    public void execute(UUID ownerId, UUID taskId) {
        if (!tasks.deleteByIdAndOwnerId(taskId, ownerId)) {
            throw new TaskNotFoundException(taskId);
        }
    }
}
