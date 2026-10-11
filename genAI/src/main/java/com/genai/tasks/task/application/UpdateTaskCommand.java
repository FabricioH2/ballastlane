package com.genai.tasks.task.application;

import com.genai.tasks.task.domain.TaskStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Full replacement of a task's editable fields. {@code ownerId} identifies the caller; it is never
 * written to the task.
 */
public record UpdateTaskCommand(UUID ownerId, UUID taskId, String title, String description, TaskStatus status,
                                LocalDate dueDate) {
}
