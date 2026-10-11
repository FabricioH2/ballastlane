package com.genai.tasks.task.web;

import com.genai.tasks.shared.application.PageQuery;
import com.genai.tasks.shared.application.PageResult;
import com.genai.tasks.shared.infrastructure.security.SecurityConfig;
import com.genai.tasks.task.application.CreateTaskCommand;
import com.genai.tasks.task.application.CreateTaskUseCase;
import com.genai.tasks.task.application.DeleteTaskUseCase;
import com.genai.tasks.task.application.GetTaskUseCase;
import com.genai.tasks.task.application.ListTasksUseCase;
import com.genai.tasks.task.application.TaskConflictException;
import com.genai.tasks.task.application.TaskNotFoundException;
import com.genai.tasks.task.application.UpdateTaskCommand;
import com.genai.tasks.task.application.UpdateTaskUseCase;
import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import com.genai.tasks.task.domain.TaskValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
class TaskControllerTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final Instant CREATED = Instant.parse("2026-10-11T10:00:00Z");
    private static final LocalDate DUE = LocalDate.of(2026, 10, 20);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreateTaskUseCase createTask;
    @MockitoBean
    private GetTaskUseCase getTask;
    @MockitoBean
    private ListTasksUseCase listTasks;
    @MockitoBean
    private UpdateTaskUseCase updateTask;
    @MockitoBean
    private DeleteTaskUseCase deleteTask;

    private static JwtRequestPostProcessor owner() {
        return jwt().jwt(token -> token.subject(OWNER.toString()));
    }

    private static Task task(String title, TaskStatus status) {
        return Task.restore(UUID.randomUUID(), OWNER, title, "desc", status, DUE, CREATED, CREATED, 0L);
    }

    @Nested
    class Create {

        @Test
        void returns201WithLocationAndBody() throws Exception {
            Task created = task("Buy milk", TaskStatus.TODO);
            when(createTask.execute(any())).thenReturn(created);

            mvc.perform(post("/api/tasks").with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "Buy milk", "description": "desc", "dueDate": "2026-10-20"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith("/api/tasks/" + created.id())))
                    .andExpect(jsonPath("$.id").value(created.id().toString()))
                    .andExpect(jsonPath("$.title").value("Buy milk"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.dueDate").value("2026-10-20"))
                    .andExpect(jsonPath("$.ownerId").doesNotExist());

            ArgumentCaptor<CreateTaskCommand> command = ArgumentCaptor.forClass(CreateTaskCommand.class);
            verify(createTask).execute(command.capture());
            assertThat(command.getValue().ownerId()).isEqualTo(OWNER);
        }

        @Test
        void returns400WithInvalidFieldsWhenBeanValidationFails() throws Exception {
            mvc.perform(post("/api/tasks").with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "  ", "description": "%s"}
                                    """.formatted("d".repeat(2001))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                    .andExpect(jsonPath("$.errors[*].field", hasItem("description")));

            verifyNoInteractions(createTask);
        }

        @Test
        void returns400WhenDomainRejectsPastDueDate() throws Exception {
            when(createTask.execute(any())).thenThrow(new TaskValidationException(
                    List.of(new TaskValidationException.Violation("dueDate", "must not be in the past"))));

            mvc.perform(post("/api/tasks").with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "Late", "dueDate": "2020-01-01"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("dueDate"))
                    .andExpect(jsonPath("$.errors[0].message").value("must not be in the past"));
        }

        @Test
        void returns400ForUnknownStatusValue() throws Exception {
            mvc.perform(post("/api/tasks").with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "t", "status": "FINISHED"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("status"))
                    .andExpect(jsonPath("$.errors[0].message").value("must be one of [TODO, IN_PROGRESS, DONE]"));

            verifyNoInteractions(createTask);
        }

        @Test
        void returns400ForMalformedJson() throws Exception {
            mvc.perform(post("/api/tasks").with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        void returns400WhenBodyTriesToSetOwner() throws Exception {
            mvc.perform(post("/api/tasks").with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "t", "ownerId": "%s"}
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("ownerId"));

            verifyNoInteractions(createTask);
        }

        @Test
        void returns401WithoutToken() throws Exception {
            mvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"t\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(header().string("WWW-Authenticate", "Bearer"));

            verifyNoInteractions(createTask);
        }

        @Test
        void returns401WhenTokenIsInvalid() throws Exception {
            mvc.perform(post("/api/tasks")
                            .header("Authorization", "Bearer not-a-jwt")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"t\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        void returns401WhenTokenSubjectIsNotAUserId() throws Exception {
            mvc.perform(post("/api/tasks").with(jwt().jwt(token -> token.subject("admin")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"t\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

            verifyNoInteractions(createTask);
        }
    }

    @Nested
    class List_ {

        @Test
        void returns200WithPageOfCallersTasks() throws Exception {
            Task task = task("Mine", TaskStatus.DONE);
            when(listTasks.execute(OWNER, TaskStatus.DONE, new PageQuery(1, 5)))
                    .thenReturn(new PageResult<>(List.of(task), 1, 5, 6));

            mvc.perform(get("/api/tasks").with(owner())
                            .param("status", "DONE").param("page", "1").param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(task.id().toString()))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalElements").value(6))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        void usesDefaultPagingWithoutParameters() throws Exception {
            when(listTasks.execute(OWNER, null, new PageQuery(0, 20)))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0));

            mvc.perform(get("/api/tasks").with(owner()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalPages").value(0));
        }

        @Test
        void returns400ForUnknownStatusFilter() throws Exception {
            mvc.perform(get("/api/tasks").with(owner()).param("status", "ARCHIVED"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("status"))
                    .andExpect(jsonPath("$.errors[0].message").value("must be one of [TODO, IN_PROGRESS, DONE]"));

            verifyNoInteractions(listTasks);
        }

        @Test
        void returns400ForPageSizeAboveLimit() throws Exception {
            mvc.perform(get("/api/tasks").with(owner()).param("size", "101"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("size"));

            verifyNoInteractions(listTasks);
        }
    }

    @Nested
    class GetOne {

        @Test
        void returns200() throws Exception {
            Task task = task("Mine", TaskStatus.IN_PROGRESS);
            when(getTask.execute(OWNER, task.id())).thenReturn(task);

            mvc.perform(get("/api/tasks/{id}", task.id()).with(owner()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Mine"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void returns404WhenTaskIsNotCallers() throws Exception {
            UUID id = UUID.randomUUID();
            when(getTask.execute(OWNER, id)).thenThrow(new TaskNotFoundException(id));

            mvc.perform(get("/api/tasks/{id}", id).with(owner()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Not Found"));
        }

        @Test
        void returns400ForMalformedId() throws Exception {
            mvc.perform(get("/api/tasks/{id}", "not-a-uuid").with(owner()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("id"));
        }
    }

    @Nested
    class Update {

        @Test
        void returns200WithUpdatedTask() throws Exception {
            Task updated = task("Renamed", TaskStatus.DONE);
            when(updateTask.execute(any())).thenReturn(updated);

            mvc.perform(put("/api/tasks/{id}", updated.id()).with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "Renamed", "status": "DONE", "dueDate": "2026-10-20"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Renamed"))
                    .andExpect(jsonPath("$.status").value("DONE"));

            verify(updateTask).execute(eq(new UpdateTaskCommand(
                    OWNER, updated.id(), "Renamed", null, TaskStatus.DONE, DUE)));
        }

        @Test
        void returns400WhenStatusIsMissing() throws Exception {
            mvc.perform(put("/api/tasks/{id}", UUID.randomUUID()).with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"t\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("status"));

            verifyNoInteractions(updateTask);
        }

        @Test
        void returns400ForUnknownStatusValue() throws Exception {
            mvc.perform(put("/api/tasks/{id}", UUID.randomUUID()).with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"t\", \"status\": \"done\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("status"));

            verifyNoInteractions(updateTask);
        }

        @Test
        void returns400AndNeverChangesOwnerWhenBodyContainsOwnerId() throws Exception {
            mvc.perform(put("/api/tasks/{id}", UUID.randomUUID()).with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "t", "status": "TODO", "ownerId": "%s"}
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("ownerId"));

            verifyNoInteractions(updateTask);
        }

        @Test
        void returns404WhenTaskIsNotCallers() throws Exception {
            UUID id = UUID.randomUUID();
            when(updateTask.execute(any())).thenThrow(new TaskNotFoundException(id));

            mvc.perform(put("/api/tasks/{id}", id).with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"t\", \"status\": \"TODO\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        void returns409WhenTaskWasChangedConcurrently() throws Exception {
            UUID id = UUID.randomUUID();
            when(updateTask.execute(any())).thenThrow(new TaskConflictException(id));

            mvc.perform(put("/api/tasks/{id}", id).with(owner())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"t\", \"status\": \"TODO\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value("Conflict"));
        }

        @Test
        void returns401WithoutToken() throws Exception {
            mvc.perform(put("/api/tasks/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"t\", \"status\": \"TODO\"}"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(updateTask);
        }
    }

    @Nested
    class Delete {

        @Test
        void returns204() throws Exception {
            UUID id = UUID.randomUUID();

            mvc.perform(delete("/api/tasks/{id}", id).with(owner()))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(deleteTask).execute(OWNER, id);
        }

        @Test
        void returns404WhenTaskIsNotCallers() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new TaskNotFoundException(id)).when(deleteTask).execute(OWNER, id);

            mvc.perform(delete("/api/tasks/{id}", id).with(owner()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        void returns401WithoutToken() throws Exception {
            mvc.perform(delete("/api/tasks/{id}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(deleteTask);
        }
    }
}
