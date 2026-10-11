package com.genai.tasks.task.application;

import com.genai.tasks.task.domain.TaskStatus;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskCommand(UUID ownerId, String title, String description, TaskStatus status,
                                LocalDate dueDate) {
}
