package com.genai.tasks.task.application;

import java.util.UUID;

/**
 * The task does not exist or belongs to another user. The two cases are deliberately indistinguishable.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(UUID taskId) {
        super("Task " + taskId + " not found");
    }
}
