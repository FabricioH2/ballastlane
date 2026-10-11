package com.genai.tasks.task.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class TaskTest {

    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);
    private static final UUID OWNER = UUID.randomUUID();

    @Nested
    class Create {

        @Test
        void createsTaskWithDefaultsAndTimestampsFromClock() {
            Task task = Task.create(OWNER, "Write tests", null, null, null, CLOCK);

            assertThat(task.id()).isNotNull();
            assertThat(task.ownerId()).isEqualTo(OWNER);
            assertThat(task.title()).isEqualTo("Write tests");
            assertThat(task.description()).isNull();
            assertThat(task.status()).isEqualTo(TaskStatus.TODO);
            assertThat(task.dueDate()).isNull();
            assertThat(task.createdAt()).isEqualTo(NOW);
            assertThat(task.updatedAt()).isEqualTo(NOW);
            assertThat(task.version()).as("a task that was never stored has no version").isNull();
        }

        @Test
        void keepsExplicitStatusAndDueDateToday() {
            Task task = Task.create(OWNER, "Ship it", "desc", TaskStatus.IN_PROGRESS, TODAY, CLOCK);

            assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(task.dueDate()).isEqualTo(TODAY);
            assertThat(task.description()).isEqualTo("desc");
        }

        @Test
        void acceptsTitleOfExactly120Characters() {
            Task task = Task.create(OWNER, "a".repeat(120), null, null, null, CLOCK);

            assertThat(task.title()).hasSize(120);
        }

        @Test
        void rejectsBlankTitle() {
            assertViolation(() -> Task.create(OWNER, "   ", null, null, null, CLOCK), "title");
        }

        @Test
        void rejectsNullTitle() {
            assertViolation(() -> Task.create(OWNER, null, null, null, null, CLOCK), "title");
        }

        @Test
        void rejectsTitleLongerThan120Characters() {
            assertViolation(() -> Task.create(OWNER, "a".repeat(121), null, null, null, CLOCK), "title");
        }

        @Test
        void rejectsDescriptionLongerThan2000Characters() {
            assertViolation(() -> Task.create(OWNER, "t", "d".repeat(2001), null, null, CLOCK), "description");
        }

        @Test
        void rejectsDueDateInThePast() {
            assertViolation(() -> Task.create(OWNER, "t", null, null, TODAY.minusDays(1), CLOCK), "dueDate");
        }

        @Test
        void todayIsDeterminedInTheClocksBusinessTimeZone() {
            // 2026-10-11T01:00Z is still 2026-10-10 20:00 in Guayaquil (UTC-5).
            Clock guayaquil = Clock.fixed(Instant.parse("2026-10-11T01:00:00Z"), ZoneId.of("America/Guayaquil"));

            Task task = Task.create(OWNER, "Due tonight", null, null, LocalDate.of(2026, 10, 10), guayaquil);

            assertThat(task.dueDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        }

        @Test
        void sameInstantInUtcIsAlreadyTheNextDay() {
            Clock utc = Clock.fixed(Instant.parse("2026-10-11T01:00:00Z"), ZoneOffset.UTC);

            assertViolation(() -> Task.create(OWNER, "Due tonight", null, null, LocalDate.of(2026, 10, 10), utc),
                    "dueDate");
        }

        @Test
        void rejectsMissingOwner() {
            assertThatThrownBy(() -> Task.create(null, "t", null, null, null, CLOCK))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void reportsEveryViolationAtOnce() {
            TaskValidationException ex = catchThrowableOfType(TaskValidationException.class,
                    () -> Task.create(OWNER, "", "d".repeat(2001), null, TODAY.minusDays(1), CLOCK));

            assertThat(ex.violations())
                    .extracting(TaskValidationException.Violation::field)
                    .containsExactlyInAnyOrder("title", "description", "dueDate");
        }
    }

    @Nested
    class Update {

        private final Instant later = NOW.plusSeconds(3600);
        private final Clock laterClock = Clock.fixed(later, ZoneOffset.UTC);

        @Test
        void replacesAllEditableFieldsAndTouchesUpdatedAt() {
            Task task = Task.create(OWNER, "Old", "old", TaskStatus.TODO, null, CLOCK);

            task.update("New", "new", TaskStatus.DONE, TODAY.plusDays(3), laterClock);

            assertThat(task.title()).isEqualTo("New");
            assertThat(task.description()).isEqualTo("new");
            assertThat(task.status()).isEqualTo(TaskStatus.DONE);
            assertThat(task.dueDate()).isEqualTo(TODAY.plusDays(3));
            assertThat(task.updatedAt()).isEqualTo(later);
            assertThat(task.createdAt()).isEqualTo(NOW);
            assertThat(task.ownerId()).isEqualTo(OWNER);
        }

        @Test
        void allowsKeepingAnExistingDueDateThatIsNowInThePast() {
            LocalDate pastDueDate = TODAY.minusDays(10);
            Task task = Task.restore(UUID.randomUUID(), OWNER, "t", null, TaskStatus.TODO, pastDueDate,
                    NOW.minusSeconds(864_000), NOW.minusSeconds(864_000), 0L);

            task.update("t2", null, TaskStatus.IN_PROGRESS, pastDueDate, CLOCK);

            assertThat(task.dueDate()).isEqualTo(pastDueDate);
            assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        void rejectsChangingDueDateToAnotherPastDate() {
            Task task = Task.create(OWNER, "t", null, null, TODAY.plusDays(1), CLOCK);

            assertViolation(() -> task.update("t", null, TaskStatus.TODO, TODAY.minusDays(1), CLOCK), "dueDate");
            assertThat(task.dueDate()).isEqualTo(TODAY.plusDays(1));
        }

        @Test
        void rejectsMissingStatus() {
            Task task = Task.create(OWNER, "t", null, null, null, CLOCK);

            assertViolation(() -> task.update("t", null, null, null, CLOCK), "status");
        }

        @Test
        void leavesTaskUntouchedWhenUpdateIsInvalid() {
            Task task = Task.create(OWNER, "Keep", "keep", TaskStatus.TODO, null, CLOCK);

            assertViolation(() -> task.update("", "changed", TaskStatus.DONE, null, laterClock), "title");

            assertThat(task.title()).isEqualTo("Keep");
            assertThat(task.description()).isEqualTo("keep");
            assertThat(task.status()).isEqualTo(TaskStatus.TODO);
            assertThat(task.updatedAt()).isEqualTo(NOW);
        }
    }

    private static void assertViolation(Runnable action, String field) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(TaskValidationException.class, ex ->
                        assertThat(ex.violations())
                                .extracting(TaskValidationException.Violation::field)
                                .contains(field));
    }
}
