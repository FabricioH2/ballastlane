package com.genai.tasks.task.infrastructure.persistence;

import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = Task.TITLE_MAX_LENGTH)
    private String title;

    @Column(length = Task.DESCRIPTION_MAX_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * {@code null} until first persisted, which also tells Spring Data the entity is new (persist, no SELECT).
     */
    @Version
    private Long version;

    protected TaskJpaEntity() {
    }

    /**
     * Maps a task that has never been stored.
     */
    static TaskJpaEntity newFrom(Task task) {
        if (task.version() != null) {
            throw new IllegalArgumentException("Task " + task.id() + " has already been stored");
        }
        TaskJpaEntity entity = new TaskJpaEntity();
        entity.id = task.id();
        entity.ownerId = task.ownerId();
        entity.title = task.title();
        entity.description = task.description();
        entity.status = task.status();
        entity.dueDate = task.dueDate();
        entity.createdAt = task.createdAt();
        entity.updatedAt = task.updatedAt();
        return entity;
    }

    /**
     * Copies the editable state of an already-stored task. Id, owner, creation time and version are never
     * taken from the domain object; Hibernate bumps the version on flush.
     */
    void apply(Task task) {
        this.title = task.title();
        this.description = task.description();
        this.status = task.status();
        this.dueDate = task.dueDate();
        this.updatedAt = task.updatedAt();
    }

    UUID ownerId() {
        return ownerId;
    }

    Long version() {
        return version;
    }

    Task toDomain() {
        return Task.restore(id, ownerId, title, description, status, dueDate, createdAt, updatedAt, version);
    }
}
