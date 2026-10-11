package com.genai.tasks.task.application;

import com.genai.tasks.shared.application.PageQuery;
import com.genai.tasks.shared.application.PageResult;
import com.genai.tasks.task.application.port.out.TaskRepository;
import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTasksUseCaseTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Mock
    private TaskRepository repository;

    private ListTasksUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListTasksUseCase(repository);
    }

    @Test
    void returnsCallersTasksForRequestedStatusAndPage() {
        PageQuery page = new PageQuery(1, 5);
        Task task = TaskFixtures.existingTask(OWNER);
        PageResult<Task> expected = new PageResult<>(List.of(task), 1, 5, 6);
        when(repository.findAllByOwner(OWNER, TaskStatus.TODO, page)).thenReturn(expected);

        assertThat(useCase.execute(OWNER, TaskStatus.TODO, page)).isSameAs(expected);
    }

    @Test
    void passesNullStatusToListEveryStatus() {
        PageQuery page = new PageQuery(0, 20);
        PageResult<Task> expected = new PageResult<>(List.of(), 0, 20, 0);
        when(repository.findAllByOwner(OWNER, null, page)).thenReturn(expected);

        assertThat(useCase.execute(OWNER, null, page)).isSameAs(expected);
    }
}
