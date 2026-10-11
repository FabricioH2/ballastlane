package com.genai.tasks.task.application;

import java.util.UUID;

/**
 * The task was changed or deleted by another request after this copy was loaded.
 */
public class TaskConflictException extends RuntimeException {

    public TaskConflictException(UUID taskId) {
        super("Task " + taskId + " was modified or deleted concurrently");
    }
}
