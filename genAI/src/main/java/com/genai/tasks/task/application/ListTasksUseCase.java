package com.genai.tasks.task.application;

import com.genai.tasks.shared.application.PageQuery;
import com.genai.tasks.shared.application.PageResult;
import com.genai.tasks.task.application.port.out.TaskRepository;
import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;

import java.util.UUID;

public class ListTasksUseCase {

    private final TaskRepository tasks;

    public ListTasksUseCase(TaskRepository tasks) {
        this.tasks = tasks;
    }

    /**
     * @param status optional filter; {@code null} lists every status
     */
    public PageResult<Task> execute(UUID ownerId, TaskStatus status, PageQuery page) {
        return tasks.findAllByOwner(ownerId, status, page);
    }
}
