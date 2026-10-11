package com.genai.tasks.task.application;

import com.genai.tasks.task.application.port.out.TaskRepository;
import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import com.genai.tasks.task.domain.TaskValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
class CreateTaskUseCaseTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Mock
    private TaskRepository repository;

    private CreateTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTaskUseCase(repository, CLOCK);
    }

    @Test
    void createsTaskOwnedByCallerAndSavesIt() {
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task created = useCase.execute(new CreateTaskCommand(OWNER, "Buy milk", "2 litres", null, TODAY.plusDays(1)));

        ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().ownerId()).isEqualTo(OWNER);
        assertThat(saved.getValue().title()).isEqualTo("Buy milk");
        assertThat(saved.getValue().status()).isEqualTo(TaskStatus.TODO);
        assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
        assertThat(created).isSameAs(saved.getValue());
    }

    @Test
    void acceptsTodaysDueDateInTheBusinessTimeZoneEvenWhenUtcIsAlreadyTomorrow() {
        Clock guayaquil = Clock.fixed(Instant.parse("2026-10-11T01:00:00Z"), ZoneId.of("America/Guayaquil"));
        CreateTaskUseCase inGuayaquil = new CreateTaskUseCase(repository, guayaquil);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task created = inGuayaquil.execute(
                new CreateTaskCommand(OWNER, "Due tonight", null, null, LocalDate.of(2026, 10, 10)));

        assertThat(created.dueDate()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    @Test
    void doesNotSaveInvalidTask() {
        CreateTaskCommand command = new CreateTaskCommand(OWNER, "t", null, null, TODAY.minusDays(1));

        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(TaskValidationException.class);
        verify(repository, never()).save(any());
    }
}
