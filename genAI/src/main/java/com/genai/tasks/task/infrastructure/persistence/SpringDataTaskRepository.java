package com.genai.tasks.task.infrastructure.persistence;

import com.genai.tasks.task.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataTaskRepository extends JpaRepository<TaskJpaEntity, UUID> {

    Optional<TaskJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<TaskJpaEntity> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<TaskJpaEntity> findByOwnerIdAndStatus(UUID ownerId, TaskStatus status, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TaskJpaEntity t where t.id = :id and t.ownerId = :ownerId")
    int deleteOwned(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
