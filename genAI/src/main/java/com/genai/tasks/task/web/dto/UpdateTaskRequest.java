package com.genai.tasks.task.web.dto;

import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Full replacement of a task's editable fields: omitted optional fields are cleared.
 *
 * @param ownerId must be absent: a task's owner can never change
 */
public record UpdateTaskRequest(
        @NotBlank @Size(max = Task.TITLE_MAX_LENGTH) String title,
        @Size(max = Task.DESCRIPTION_MAX_LENGTH) String description,
        @NotNull TaskStatus status,
        LocalDate dueDate,
        @Null(message = OwnerField.MESSAGE) Object ownerId) {
}
