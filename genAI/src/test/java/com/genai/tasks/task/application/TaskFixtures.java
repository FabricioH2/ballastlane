package com.genai.tasks.task.application;

import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

final class TaskFixtures {

    static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    private TaskFixtures() {
    }

    static Task existingTask(UUID ownerId) {
        Instant created = NOW.minusSeconds(86_400);
        return Task.restore(UUID.randomUUID(), ownerId, "Existing", "existing description",
                TaskStatus.TODO, TODAY.plusDays(5), created, created, 0L);
    }
}
