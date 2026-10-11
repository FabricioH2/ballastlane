package com.genai.tasks.task.application;

import com.genai.tasks.task.application.port.out.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteTaskUseCaseTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Mock
    private TaskRepository repository;

    private DeleteTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteTaskUseCase(repository);
    }

    @Test
    void deletesCallersTask() {
        UUID taskId = UUID.randomUUID();
        when(repository.deleteByIdAndOwnerId(taskId, OWNER)).thenReturn(true);

        assertThatCode(() -> useCase.execute(OWNER, taskId)).doesNotThrowAnyException();
        verify(repository).deleteByIdAndOwnerId(taskId, OWNER);
    }

    @Test
    void throwsNotFoundWhenNothingOwnedByCallerWasDeleted() {
        UUID taskId = UUID.randomUUID();
        when(repository.deleteByIdAndOwnerId(taskId, OWNER)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(OWNER, taskId)).isInstanceOf(TaskNotFoundException.class);
    }
}
