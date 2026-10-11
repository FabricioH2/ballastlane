package com.genai.tasks.task.infrastructure.persistence;

import com.genai.tasks.shared.application.PageQuery;
import com.genai.tasks.shared.application.PageResult;
import com.genai.tasks.task.application.TaskConflictException;
import com.genai.tasks.task.domain.Task;
import com.genai.tasks.task.domain.TaskStatus;
import com.genai.tasks.testsupport.PostgresTestcontainer;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestcontainer.class, JpaTaskRepositoryAdapter.class})
class JpaTaskRepositoryAdapterIT {

    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    @Autowired
    private JpaTaskRepositoryAdapter repository;

    @Autowired
    private EntityManager entityManager;

    private UUID alice;
    private UUID bob;

    @BeforeEach
    void createUsers() {
        alice = insertUser("alice");
        bob = insertUser("bob");
    }

    @Test
    void savesAndLoadsEveryField() {
        Task task = Task.create(alice, "Write report", "Quarterly numbers", TaskStatus.IN_PROGRESS,
                TODAY.plusDays(3), clockAt(NOW));

        repository.save(task);
        flushAndClear();

        Task loaded = repository.findByIdAndOwnerId(task.id(), alice).orElseThrow();
        assertThat(loaded.id()).isEqualTo(task.id());
        assertThat(loaded.ownerId()).isEqualTo(alice);
        assertThat(loaded.title()).isEqualTo("Write report");
        assertThat(loaded.description()).isEqualTo("Quarterly numbers");
        assertThat(loaded.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(loaded.dueDate()).isEqualTo(TODAY.plusDays(3));
        assertThat(loaded.createdAt()).isEqualTo(NOW);
        assertThat(loaded.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void persistsUpdatesToAnExistingTask() {
        Task task = repository.save(Task.create(alice, "Draft", null, null, null, clockAt(NOW)));
        flushAndClear();

        Task loaded = repository.findByIdAndOwnerId(task.id(), alice).orElseThrow();
        loaded.update("Final", "done", TaskStatus.DONE, null, clockAt(NOW.plusSeconds(60)));
        repository.save(loaded);
        flushAndClear();

        Task reloaded = repository.findByIdAndOwnerId(task.id(), alice).orElseThrow();
        assertThat(reloaded.title()).isEqualTo("Final");
        assertThat(reloaded.status()).isEqualTo(TaskStatus.DONE);
        assertThat(reloaded.createdAt()).isEqualTo(NOW);
        assertThat(reloaded.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void doesNotFindAnotherUsersTask() {
        Task task = repository.save(Task.create(alice, "Private", null, null, null, clockAt(NOW)));
        flushAndClear();

        assertThat(repository.findByIdAndOwnerId(task.id(), bob)).isEmpty();
    }

    @Test
    void listsOnlyOwnersTasksNewestFirstWithPaging() {
        repository.save(Task.create(alice, "first", null, null, null, clockAt(NOW)));
        repository.save(Task.create(alice, "second", null, null, null, clockAt(NOW.plusSeconds(1))));
        repository.save(Task.create(alice, "third", null, null, null, clockAt(NOW.plusSeconds(2))));
        repository.save(Task.create(bob, "bob's", null, null, null, clockAt(NOW.plusSeconds(3))));
        flushAndClear();

        PageResult<Task> firstPage = repository.findAllByOwner(alice, null, new PageQuery(0, 2));
        PageResult<Task> secondPage = repository.findAllByOwner(alice, null, new PageQuery(1, 2));

        assertThat(firstPage.content()).extracting(Task::title).containsExactly("third", "second");
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).extracting(Task::title).containsExactly("first");
        assertThat(secondPage.page()).isEqualTo(1);
    }

    @Test
    void filtersByStatus() {
        repository.save(Task.create(alice, "todo", null, TaskStatus.TODO, null, clockAt(NOW)));
        repository.save(Task.create(alice, "done", null, TaskStatus.DONE, null, clockAt(NOW)));
        repository.save(Task.create(bob, "bob done", null, TaskStatus.DONE, null, clockAt(NOW)));
        flushAndClear();

        PageResult<Task> done = repository.findAllByOwner(alice, TaskStatus.DONE, new PageQuery(0, 20));

        assertThat(done.content()).extracting(Task::title).containsExactly("done");
        assertThat(done.totalElements()).isEqualTo(1);
    }

    @Test
    void deletesOnlyWhenOwnerMatches() {
        Task task = repository.save(Task.create(alice, "keep me", null, null, null, clockAt(NOW)));
        flushAndClear();

        assertThat(repository.deleteByIdAndOwnerId(task.id(), bob)).isFalse();
        flushAndClear();
        assertThat(repository.findByIdAndOwnerId(task.id(), alice)).isPresent();

        assertThat(repository.deleteByIdAndOwnerId(task.id(), alice)).isTrue();
        flushAndClear();
        assertThat(repository.findByIdAndOwnerId(task.id(), alice)).isEmpty();
    }

    @Test
    void deleteOfUnknownIdReturnsFalse() {
        assertThat(repository.deleteByIdAndOwnerId(UUID.randomUUID(), alice)).isFalse();
    }

    @Test
    void creatingATaskIssuesASingleInsertAndNoSelect() {
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        repository.save(Task.create(alice, "New", null, null, null, clockAt(NOW)));
        entityManager.flush();

        assertThat(statistics.getPrepareStatementCount())
                .as("JDBC statements for creating one task")
                .isEqualTo(1);
    }

    @Test
    void versionStartsAtZeroAndIncrementsOnEveryUpdate() {
        Task created = repository.save(Task.create(alice, "v0", null, null, null, clockAt(NOW)));
        flushAndClear();
        assertThat(created.version()).isZero();

        Task loaded = repository.findByIdAndOwnerId(created.id(), alice).orElseThrow();
        loaded.update("v1", null, TaskStatus.TODO, null, clockAt(NOW));
        Task updated = repository.save(loaded);
        flushAndClear();

        assertThat(updated.version()).isEqualTo(1);
        assertThat(repository.findByIdAndOwnerId(created.id(), alice).orElseThrow().version()).isEqualTo(1);
    }

    @Test
    void secondOfTwoConcurrentUpdatesIsRejectedInsteadOfOverwritingTheFirst() {
        Task created = repository.save(Task.create(alice, "Original", null, null, null, clockAt(NOW)));
        flushAndClear();
        Task firstCopy = repository.findByIdAndOwnerId(created.id(), alice).orElseThrow();
        Task secondCopy = repository.findByIdAndOwnerId(created.id(), alice).orElseThrow();
        flushAndClear();

        firstCopy.update("First writer", null, TaskStatus.DONE, null, clockAt(NOW));
        repository.save(firstCopy);
        flushAndClear();

        secondCopy.update("Second writer", null, TaskStatus.TODO, null, clockAt(NOW));
        assertThatThrownBy(() -> repository.save(secondCopy)).isInstanceOf(TaskConflictException.class);
        flushAndClear();

        Task stored = repository.findByIdAndOwnerId(created.id(), alice).orElseThrow();
        assertThat(stored.title()).isEqualTo("First writer");
        assertThat(stored.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void changeCommittedByAnotherTransactionIsDetectedAtWriteTime() {
        Task created = repository.save(Task.create(alice, "Original", null, null, null, clockAt(NOW)));
        flushAndClear();
        // Loads the row into the persistence context at version 0...
        Task loaded = repository.findByIdAndOwnerId(created.id(), alice).orElseThrow();
        // ...then another transaction commits a change behind its back.
        entityManager.createNativeQuery("UPDATE tasks SET title = 'Other writer', version = version + 1 WHERE id = ?1")
                .setParameter(1, created.id())
                .executeUpdate();

        loaded.update("Stale writer", null, TaskStatus.TODO, null, clockAt(NOW));

        assertThatThrownBy(() -> repository.save(loaded)).isInstanceOf(TaskConflictException.class);
    }

    @Test
    void updateOfATaskDeletedInTheMeantimeDoesNotResurrectIt() {
        Task created = repository.save(Task.create(alice, "Doomed", null, null, null, clockAt(NOW)));
        flushAndClear();
        Task loaded = repository.findByIdAndOwnerId(created.id(), alice).orElseThrow();
        flushAndClear();
        assertThat(repository.deleteByIdAndOwnerId(created.id(), alice)).isTrue();
        flushAndClear();

        loaded.update("Back from the dead", null, TaskStatus.TODO, null, clockAt(NOW));

        assertThatThrownBy(() -> repository.save(loaded)).isInstanceOf(TaskConflictException.class);
        flushAndClear();
        assertThat(repository.findByIdAndOwnerId(created.id(), alice)).isEmpty();
    }

    private UUID insertUser(String username) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery("INSERT INTO users (id, username, password_hash) VALUES (?1, ?2, ?3)")
                .setParameter(1, id)
                .setParameter(2, username + "-" + id)
                .setParameter(3, "irrelevant")
                .executeUpdate();
        return id;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
