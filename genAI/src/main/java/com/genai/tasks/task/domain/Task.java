package com.genai.tasks.task.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A unit of work owned by exactly one user. The owner is fixed at creation and can never change.
 */
public final class Task {

    public static final int TITLE_MAX_LENGTH = 120;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    private final UUID id;
    private final UUID ownerId;
    private final Instant createdAt;
    private final Long version;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDate dueDate;
    private Instant updatedAt;

    private Task(UUID id, UUID ownerId, String title, String description, TaskStatus status,
                 LocalDate dueDate, Instant createdAt, Instant updatedAt, Long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.title = title;
        this.description = description;
        this.status = Objects.requireNonNull(status, "status");
        this.dueDate = dueDate;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.version = version;
    }

    /**
     * Creates a new task. A missing status defaults to {@link TaskStatus#TODO}; the due date, if given,
     * must not be before today according to {@code clock}.
     */
    public static Task create(UUID ownerId, String title, String description, TaskStatus status,
                              LocalDate dueDate, Clock clock) {
        Objects.requireNonNull(ownerId, "ownerId");
        LocalDate today = LocalDate.now(clock);
        List<TaskValidationException.Violation> violations = new ArrayList<>();
        validateTitle(title, violations);
        validateDescription(description, violations);
        if (dueDate != null && dueDate.isBefore(today)) {
            violations.add(new TaskValidationException.Violation("dueDate", "must not be in the past"));
        }
        throwIfAny(violations);

        Instant now = clock.instant();
        return new Task(UUID.randomUUID(), ownerId, title, description,
                status == null ? TaskStatus.TODO : status, dueDate, now, now, null);
    }

    /**
     * Rebuilds a task from storage. No business rules are re-checked: a stored due date may legitimately
     * be in the past by now.
     *
     * @param version the stored version this copy was read at, used to detect concurrent modification
     */
    public static Task restore(UUID id, UUID ownerId, String title, String description, TaskStatus status,
                               LocalDate dueDate, Instant createdAt, Instant updatedAt, Long version) {
        return new Task(id, ownerId, title, description, status, dueDate, createdAt, updatedAt,
                Objects.requireNonNull(version, "version"));
    }

    /**
     * Replaces every editable field. The owner and creation time are never touched. A due date that is
     * already in the past may be kept, but the due date cannot be moved to a different past date.
     * The task is left unchanged if any rule is violated.
     */
    public void update(String title, String description, TaskStatus status, LocalDate dueDate, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        List<TaskValidationException.Violation> violations = new ArrayList<>();
        validateTitle(title, violations);
        validateDescription(description, violations);
        if (status == null) {
            violations.add(new TaskValidationException.Violation("status", "must not be null"));
        }
        if (dueDate != null && !dueDate.equals(this.dueDate) && dueDate.isBefore(today)) {
            violations.add(new TaskValidationException.Violation("dueDate", "must not be in the past"));
        }
        throwIfAny(violations);

        this.title = title;
        this.description = description;
        this.status = status;
        this.dueDate = dueDate;
        this.updatedAt = clock.instant();
    }

    private static void validateTitle(String title, List<TaskValidationException.Violation> violations) {
        if (title == null || title.isBlank()) {
            violations.add(new TaskValidationException.Violation("title", "must not be blank"));
        } else if (title.length() > TITLE_MAX_LENGTH) {
            violations.add(new TaskValidationException.Violation("title",
                    "size must be between 1 and " + TITLE_MAX_LENGTH));
        }
    }

    private static void validateDescription(String description, List<TaskValidationException.Violation> violations) {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            violations.add(new TaskValidationException.Violation("description",
                    "size must be between 0 and " + DESCRIPTION_MAX_LENGTH));
        }
    }

    private static void throwIfAny(List<TaskValidationException.Violation> violations) {
        if (!violations.isEmpty()) {
            throw new TaskValidationException(violations);
        }
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public TaskStatus status() {
        return status;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /**
     * @return the version this copy was loaded at, or {@code null} if the task has never been stored
     */
    public Long version() {
        return version;
    }
}
