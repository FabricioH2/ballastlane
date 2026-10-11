package com.genai.tasks.task.application;

import com.genai.tasks.task.application.port.out.TaskRepository;
import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import com.genai.tasks.task.domain.TaskValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.genai.tasks.task.application.TaskFixtures.CLOCK;
import static com.genai.tasks.task.application.TaskFixtures.NOW;
import static com.genai.tasks.task.application.TaskFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTaskUseCaseTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Mock
    private TaskRepository repository;

    private UpdateTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTaskUseCase(repository, CLOCK);
    }

    @Test
    void updatesCallersTaskAndSavesIt() {
        Task task = TaskFixtures.existingTask(OWNER);
        when(repository.findByIdAndOwnerId(task.id(), OWNER)).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);

        Task updated = useCase.execute(new UpdateTaskCommand(
                OWNER, task.id(), "Renamed", null, TaskStatus.DONE, TODAY.plusDays(2)));

        assertThat(updated.title()).isEqualTo("Renamed");
        assertThat(updated.description()).isNull();
        assertThat(updated.status()).isEqualTo(TaskStatus.DONE);
        assertThat(updated.dueDate()).isEqualTo(TODAY.plusDays(2));
        assertThat(updated.updatedAt()).isEqualTo(NOW);
        assertThat(updated.ownerId()).isEqualTo(OWNER);
        verify(repository).save(task);
    }

    @Test
    void throwsNotFoundAndSavesNothingWhenTaskIsNotCallers() {
        UUID taskId = UUID.randomUUID();
        when(repository.findByIdAndOwnerId(taskId, OWNER)).thenReturn(Optional.empty());

        UpdateTaskCommand command = new UpdateTaskCommand(OWNER, taskId, "x", null, TaskStatus.TODO, null);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(TaskNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void doesNotSaveInvalidChanges() {
        Task task = TaskFixtures.existingTask(OWNER);
        when(repository.findByIdAndOwnerId(task.id(), OWNER)).thenReturn(Optional.of(task));

        UpdateTaskCommand command = new UpdateTaskCommand(OWNER, task.id(), " ", null, TaskStatus.TODO, null);

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(TaskValidationException.class);
        verify(repository, never()).save(any());
    }
}
