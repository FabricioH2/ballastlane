package com.genai.tasks.task.infrastructure.persistence;

import com.genai.tasks.shared.application.PageQuery;
import com.genai.tasks.shared.application.PageResult;
import com.genai.tasks.task.application.TaskConflictException;
import com.genai.tasks.task.application.port.out.TaskRepository;
import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTaskRepositoryAdapter implements TaskRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final SpringDataTaskRepository jpa;

    JpaTaskRepositoryAdapter(SpringDataTaskRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * New tasks are persisted with a single INSERT. Stored tasks are updated only if the row still exists,
     * still belongs to the same owner and is still at the version the task was loaded at; the UPDATE also
     * carries {@code WHERE version = ?} to catch a writer that commits in between.
     */
    @Override
    @Transactional
    public Task save(Task task) {
        if (task.version() == null) {
            return jpa.saveAndFlush(TaskJpaEntity.newFrom(task)).toDomain();
        }
        try {
            TaskJpaEntity entity = jpa.findById(task.id())
                    .filter(stored -> stored.ownerId().equals(task.ownerId()))
                    .filter(stored -> stored.version().equals(task.version()))
                    .orElseThrow(() -> new TaskConflictException(task.id()));
            entity.apply(task);
            jpa.flush();
            return entity.toDomain();
        } catch (OptimisticLockingFailureException ex) {
            throw new TaskConflictException(task.id());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Task> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return jpa.findByIdAndOwnerId(id, ownerId).map(TaskJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Task> findAllByOwner(UUID ownerId, TaskStatus status, PageQuery page) {
        PageRequest pageable = PageRequest.of(page.page(), page.size(), NEWEST_FIRST);
        Page<TaskJpaEntity> result = status == null
                ? jpa.findByOwnerId(ownerId, pageable)
                : jpa.findByOwnerIdAndStatus(ownerId, status, pageable);
        return new PageResult<>(result.getContent().stream().map(TaskJpaEntity::toDomain).toList(),
                page.page(), page.size(), result.getTotalElements());
    }

    @Override
    @Transactional
    public boolean deleteByIdAndOwnerId(UUID id, UUID ownerId) {
        return jpa.deleteOwned(id, ownerId) > 0;
    }
}
