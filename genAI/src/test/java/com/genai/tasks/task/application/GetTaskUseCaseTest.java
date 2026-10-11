package com.genai.tasks.task.application;

import com.genai.tasks.task.application.port.out.TaskRepository;
import com.genai.tasks.task.domain.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTaskUseCaseTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Mock
    private TaskRepository repository;

    private GetTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTaskUseCase(repository);
    }

    @Test
    void returnsTaskWhenItBelongsToCaller() {
        Task task = TaskFixtures.existingTask(OWNER);
        when(repository.findByIdAndOwnerId(task.id(), OWNER)).thenReturn(Optional.of(task));

        assertThat(useCase.execute(OWNER, task.id())).isSameAs(task);
    }

    @Test
    void throwsNotFoundWhenTaskIsMissingOrOwnedBySomeoneElse() {
        UUID taskId = UUID.randomUUID();
        when(repository.findByIdAndOwnerId(taskId, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(OWNER, taskId))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining(taskId.toString());
        verify(repository).findByIdAndOwnerId(taskId, OWNER);
        verifyNoMoreInteractions(repository);
    }
}
