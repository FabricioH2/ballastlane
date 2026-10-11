package com.genai.tasks.task.web;

import com.genai.tasks.shared.application.PageQuery;
import com.genai.tasks.shared.web.CurrentUser;
import com.genai.tasks.task.application.CreateTaskCommand;
import com.genai.tasks.task.application.CreateTaskUseCase;
import com.genai.tasks.task.application.DeleteTaskUseCase;
import com.genai.tasks.task.application.GetTaskUseCase;
import com.genai.tasks.task.application.ListTasksUseCase;
import com.genai.tasks.task.application.UpdateTaskCommand;
import com.genai.tasks.task.application.UpdateTaskUseCase;
import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import com.genai.tasks.task.web.dto.CreateTaskRequest;
import com.genai.tasks.task.web.dto.PageResponse;
import com.genai.tasks.task.web.dto.TaskResponse;
import com.genai.tasks.task.web.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Every operation is scoped to the caller taken from the JWT. A task owned by someone else is reported
 * as 404, exactly like a task that does not exist.
 */
@RestController
@RequestMapping("/api/tasks")
class TaskController {

    private final CreateTaskUseCase createTask;
    private final GetTaskUseCase getTask;
    private final ListTasksUseCase listTasks;
    private final UpdateTaskUseCase updateTask;
    private final DeleteTaskUseCase deleteTask;

    TaskController(CreateTaskUseCase createTask, GetTaskUseCase getTask, ListTasksUseCase listTasks,
                   UpdateTaskUseCase updateTask, DeleteTaskUseCase deleteTask) {
        this.createTask = createTask;
        this.getTask = getTask;
        this.listTasks = listTasks;
        this.updateTask = updateTask;
        this.deleteTask = deleteTask;
    }

    @PostMapping
    ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody CreateTaskRequest request,
                                        UriComponentsBuilder uriBuilder) {
        Task task = createTask.execute(new CreateTaskCommand(CurrentUser.id(jwt), request.title(),
                request.description(), request.status(), request.dueDate()));
        URI location = uriBuilder.path("/api/tasks/{id}").buildAndExpand(task.id()).toUri();
        return ResponseEntity.created(location).body(TaskResponse.from(task));
    }

    @GetMapping
    PageResponse<TaskResponse> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(name = "status", required = false) TaskStatus status,
                                    @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                    @RequestParam(name = "size", defaultValue = "20")
                                    @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listTasks.execute(CurrentUser.id(jwt), status, new PageQuery(page, size))
                .map(TaskResponse::from));
    }

    @GetMapping("/{id}")
    TaskResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return TaskResponse.from(getTask.execute(CurrentUser.id(jwt), id));
    }

    @PutMapping("/{id}")
    TaskResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                        @Valid @RequestBody UpdateTaskRequest request) {
        Task task = updateTask.execute(new UpdateTaskCommand(CurrentUser.id(jwt), id, request.title(),
                request.description(), request.status(), request.dueDate()));
        return TaskResponse.from(task);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        deleteTask.execute(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
