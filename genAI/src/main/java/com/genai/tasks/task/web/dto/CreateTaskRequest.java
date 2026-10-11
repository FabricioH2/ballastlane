package com.genai.tasks.task.web.dto;

import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * @param status  optional, defaults to TODO
 * @param ownerId must be absent: the owner always comes from the access token
 */
public record CreateTaskRequest(
        @NotBlank @Size(max = Task.TITLE_MAX_LENGTH) String title,
        @Size(max = Task.DESCRIPTION_MAX_LENGTH) String description,
        TaskStatus status,
        LocalDate dueDate,
        @Null(message = OwnerField.MESSAGE) Object ownerId) {
}
