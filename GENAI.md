# Generative AI exercise: task management API

The brief asks for three things: the prompt I would use to generate a task management REST API, the code it produced, and how I validated and corrected it. I ran the prompt for real with **Claude Code**, in a separate, empty folder, and reviewed the result before accepting it.

| Round | What happened | Tests |
|---|---|---|
| 1. Plan | The AI listed files and assumptions and waited. I corrected 4 points before any code existed. | – |
| 2. Generation | The code was generated and all tests passed. | 75 / 75 |
| 3. My review | I found 3 real bugs that no test covered. | 75 / 75 (still green) |
| 4. Fixes, test-first | Each bug got a failing test first, then the fix. | 87 / 87 |

The most useful lesson is round 3: **a green test suite only proves what the tests check.**

---

## 1. Tool and workflow

- **Tool:** Claude Code (an agent that reads and writes files and runs commands in a folder).
- **Plan before code.** The prompt asks for the file list and assumptions first. I saved that plan as `PLAN.md` and answered its questions before any code was written. Changing direction at that point costs one message instead of twenty files.
- **The tests are the verification, not the AI's summary.** I asked it to run `./gradlew test` and show the result. I then read the code with a checklist (section 4), because passing tests only cover what someone thought of testing.
- **Fixes are requested test-first:** first a test that fails and proves the bug, then the fix.

The same way of working was used for the main Pokédex project, with a [`CLAUDE.md`](CLAUDE.md) file that records the architecture rules so the AI does not have to guess them.

---

## 2. The prompt

```text
You are a senior Java backend engineer. Generate a REST API for a simple task management system.

Stack and architecture
- Java 25, Spring Boot, Gradle, PostgreSQL, Flyway, Spring Data JPA, Spring Security with JWT.
- Clean Architecture: domain (no framework imports), application (use cases + ports),
  infrastructure (JPA, security), web (controllers, DTOs). Dependencies point inward.

Domain
- Task: id, title (required, 1-120 chars), description (optional, max 2000 chars),
  status (enum TODO, IN_PROGRESS, DONE; default TODO), dueDate (LocalDate, optional,
  cannot be in the past when created), createdAt, updatedAt, ownerId.
- A User model already exists (id, username, passwordHash). Every task belongs to exactly one user.

API (all endpoints require a valid JWT)
- POST   /api/tasks                         -> 201 + Location header
- GET    /api/tasks?status=&page=&size=     -> 200, paginated, only the caller's tasks
- GET    /api/tasks/{id}                    -> 200 / 404
- PUT    /api/tasks/{id}                    -> 200 / 400 / 404
- DELETE /api/tasks/{id}                    -> 204 / 404
- A user must never read or change another user's task: answer 404 (not 403) so ids are not leaked.

Errors and validation
- Bean Validation on request DTOs. RFC 7807 ProblemDetail for every error, with the invalid fields on 400.
- Never return JPA entities from controllers.

Tests (write them first, TDD style)
- Unit tests for the use cases with Mockito.
- @WebMvcTest for the controller: 201, 400, 401, 404, 204.
- A Testcontainers test for the repository.
- A test proving that user A cannot read or change user B's task.

Output
- First list the files you will create and your assumptions, and wait for my confirmation.
- Then generate the code file by file. No placeholders like "// TODO implement".
```

### Why it is written this way

| Part of the prompt | What it prevents |
|---|---|
| Role, stack and architecture | Random versions and structure, such as all the logic inside the controller |
| Exact limits (1–120 chars, the enum, no past date) | Whatever is not specified, the AI invents or skips |
| One HTTP status per endpoint | A result that cannot be checked point by point |
| "404, not 403, for another user's task" | The most common security bug in generated APIs: not checking who owns the resource (IDOR) |
| "Never return JPA entities" | Leaking internal fields and coupling the API to the database |
| Tests first, including "A cannot see B's tasks" | Accepting code with no way to verify it |
| "List the files and wait for my confirmation" | Twenty files written in the wrong direction |
| "No placeholders" | Unfinished code that looks finished |

---

## 3. Generated code (sample)

The full result has 67 Java files (main code and tests). These excerpts show the parts that matter most.

**Ownership is part of every query.** The port does not even offer a lookup by id alone:

```java
public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findByIdAndOwnerId(UUID id, UUID ownerId);
    PageResult<Task> findAllByOwner(UUID ownerId, TaskStatus status, PageQuery page);
    boolean deleteByIdAndOwnerId(UUID id, UUID ownerId);
}
```

**The domain owns its rules,** with an injected `Clock` so that "today" can be tested:

```java
public void update(String title, String description, TaskStatus status, LocalDate dueDate, Clock clock) {
    LocalDate today = LocalDate.now(clock);
    List<TaskValidationException.Violation> violations = new ArrayList<>();
    validateTitle(title, violations);
    validateDescription(description, violations);
    if (status == null) {
        violations.add(new TaskValidationException.Violation("status", "must not be null"));
    }
    // An existing due date that is now in the past may be kept, but not moved to another past date.
    if (dueDate != null && !dueDate.equals(this.dueDate) && dueDate.isBefore(today)) {
        violations.add(new TaskValidationException.Violation("dueDate", "must not be in the past"));
    }
    throwIfAny(violations);
    // ... assign the fields and updatedAt
}
```

**The owner always comes from the token,** never from the body:

```java
@PutMapping("/{id}")
TaskResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id,
                    @Valid @RequestBody UpdateTaskRequest request) {
    Task task = updateTask.execute(new UpdateTaskCommand(CurrentUser.id(jwt), id, request.title(),
            request.description(), request.status(), request.dueDate()));
    return TaskResponse.from(task);
}
```

**The isolation test** runs the whole application with real JWTs:

```java
@Test
void userBCannotReadUpdateOrDeleteUserAsTask() throws Exception {
    String taskId = createTask(aliceToken, "Alice's secret plan");

    mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(bobToken)))
            .andExpect(status().isNotFound())
            .andExpect(content().string(not(containsString("secret plan"))));
    // ... PUT and DELETE by Bob also return 404

    // Alice's task still exists and is untouched.
    mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(aliceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Alice's secret plan"));
}
```

---

## 4. How I validated it

1. **I read the plan before saying "go"** and corrected it (section 5, round 1).
2. **I ran the tests:** 75 tests, all passing. The tests that need dates use a fixed `Clock`, so they will not break as time passes.
3. **I reviewed the code against a checklist:**

| Check | Result |
|---|---|
| Compiles, tests pass, and the tests check behaviour (not only mocks) | ✅ Unit, `@WebMvcTest`, Testcontainers and a full-application test |
| Versions and APIs exist in the version used | ✅ After my correction to Spring Boot 4.1 |
| Every GET/PUT/DELETE checks the owner | ✅ In the query itself, with a test for each verb |
| No endpoint is accidentally public | ✅ Only `POST /api/auth/token` |
| Validation: blank or 121-char title, unknown status, past date, huge page size | ✅ All return 400 with the field that failed |
| Every error is a ProblemDetail, with no stack traces | ✅ Including 401, through a custom entry point |
| Domain free of Spring/JPA, no logic in controllers | ✅ The AI also added an ArchUnit test that fails the build otherwise |
| Flyway migrations, `ddl-auto: validate`, no N+1, indexes | ✅ |
| **Concurrent requests** | ❌ Bug 1 below |
| **Dates and time zones** | ❌ Bug 2 below |
| **Information leaks beyond the response body** | ❌ Bug 3 below |

4. **I traced the persistence calls by hand.** That is how I found bug 1: no test exercised two requests at the same time.

---

## 5. What I corrected and why

### Round 1: corrections to the plan, before any code

| The AI proposed | I asked for | Why |
|---|---|---|
| Spring Boot 4.0.x | The latest 4.1.x | Generated code tends to pick older versions than the current one |
| "UUIDs can't be guessed, which helps the 404 rule" | UUIDs are fine, but every GET/PUT/DELETE must still check the owner, with a test for all three | Unguessable ids are not access control. An id can leak through logs, links or screenshots. |
| (not mentioned) | A `PUT` must never change the owner | Otherwise a body with `"ownerId": "<someone else>"` hands the task to another user |
| (not mentioned) | An unknown `status` returns 400 with a ProblemDetail, not 500 | Bad input is the client's error, not a server failure |

The AI also asked how clients get a token, since the brief assumes a `User` model exists but says nothing about login. I accepted a minimal `POST /api/auth/token`. Without it nothing can be tested end to end.

### Round 2: three bugs found in review (all 75 tests were green)

**Bug 1: a deleted task could come back, and two edits silently overwrote each other.**

`UpdateTaskUseCase` loaded the task in one transaction and saved it in another, and `save()` called `merge()` on a new entity instance:

```java
// Generated: save() for both insert and update
return jpa.save(TaskJpaEntity.fromDomain(task)).toDomain();   // merge() of a detached entity
```

- If another request deleted the task between those two steps, `merge()` found no row and **inserted it again**.
- If two people edited the same task, the last one won and the first lost their changes without being told.
- As a side effect, every create ran an extra `SELECT` before the `INSERT`.

**Fix:** optimistic locking. I asked for a `version` column (migration `V3`) and `@Version` on the entity. The version travels in the domain `Task` as a plain `Long`, with no framework import. An update is written only if the row still exists, still has the same owner and is still at the version that was read. Otherwise the API returns **409 Conflict**. New tasks are now a single `INSERT`. The tests written first:

```java
@Test
void updateOfATaskDeletedInTheMeantimeDoesNotResurrectIt() {
    Task created = repository.save(Task.create(alice, "Doomed", null, null, null, clockAt(NOW)));
    Task loaded = repository.findByIdAndOwnerId(created.id(), alice).orElseThrow();
    assertThat(repository.deleteByIdAndOwnerId(created.id(), alice)).isTrue();

    loaded.update("Back from the dead", null, TaskStatus.TODO, null, clockAt(NOW));

    assertThatThrownBy(() -> repository.save(loaded)).isInstanceOf(TaskConflictException.class);
    assertThat(repository.findByIdAndOwnerId(created.id(), alice)).isEmpty();
}
```

There are also tests for "the second of two concurrent updates is rejected" and "creating a task issues a single INSERT and no SELECT".

**Bug 2: "today" was the date in UTC.**

The due-date rule compared against `Clock.systemUTC()`. In Guayaquil (UTC-5), at 8 pm on 10 October it is already 11 October in UTC, so a task due "today" was rejected as being in the past.

**Fix:** a configurable business time zone (`app.time-zone`, default UTC). It decides what "today" is; stored instants are unaffected. The test:

```java
@Test
void acceptsTodaysDueDateInTheBusinessTimeZoneEvenWhenUtcIsAlreadyTomorrow() {
    Clock guayaquil = Clock.fixed(Instant.parse("2026-10-11T01:00:00Z"), ZoneId.of("America/Guayaquil"));
    // ... a task due 2026-10-10 is accepted
}
```

**Bug 3: the login revealed which usernames exist, through its response time.**

The error message was the same for an unknown user and a wrong password, which is correct. But for an unknown user no BCrypt check ran, so that answer came back much faster. Timing the responses is enough to tell which usernames are registered.

**Fix:** when the user does not exist, check the password against a dummy BCrypt hash made with the same cost factor, so both paths take the same time. A unit test checks that exactly one password check runs in both cases.

After the fixes: **87 tests, all passing.**

---

## 6. Edge cases, authentication and validation

| Case | Behaviour | Where it is enforced |
|---|---|---|
| Another user's task (GET/PUT/DELETE) | 404, identical to a missing id | The query (`...AndOwnerId`) + the full-application test |
| `ownerId` in the request body | 400 | `@Null` on the DTO; the owner always comes from the JWT `sub` |
| No token, invalid or expired token, `sub` that is not a user id | 401 ProblemDetail | Spring Security resource server + custom entry point |
| Unknown login / wrong password | 401 with the same message and the same response time | Use case + dummy hash |
| Blank or too-long title, too-long description | 400 listing the fields | Bean Validation on the DTO and again in the domain |
| Unknown `status` (body or filter) | 400 "must be one of [TODO, IN_PROGRESS, DONE]" | Error handler for unreadable JSON / type mismatch |
| Past due date on create | 400 | Domain, using the business time zone |
| Keeping a due date that has since passed | Allowed; moving it to another past date is not | Domain |
| `size` above 100, negative `page`, malformed UUID | 400 | Method validation on the controller |
| Two concurrent edits / edit of a deleted task | 409 Conflict | `@Version` + the adapter |
| No JWT secret configured | The application refuses to start | `JwtProperties` |

**Known limits I would mention in a review:**
- The time zone is one setting for the whole API. Users in different zones would need the zone per user or per request.
- A token for a user deleted after login is valid until it expires.
- Usernames are case-sensitive.

---

## 7. Lessons from this project

The same review habits caught real AI mistakes while building the Pokédex API:

| What the AI got wrong | How it was caught |
|---|---|
| `BeanConfig` without the `PokeApiClient` bean and with a duplicated `RestClient`: the app did not start | The test that starts the whole application; the unit tests passed |
| Property `connectTimeOut` (capital O): Spring silently ignored the YAML value | Code review of the property names |
| Spring Boot 4 moved the test annotations (`@WebMvcTest`, `@DataJpaTest`) to new packages | Compiler error + official documentation |
| DTO field names that did not match the PokeAPI JSON, so values arrived as `null` | WireMock tests using real JSON responses |
| PostgreSQL 18 changed the data path of the Docker volume | Official image documentation |
| A `Docker/` folder that works on Windows and fails in Linux builds (case-sensitive paths) | Building the image |

Reviewing this exercise also made me check my own code: the Pokédex login (`LoginServiceImpl`) has the same timing leak as bug 3. It is listed under next steps in the presentation.

**General lesson:** the AI speeds things up a lot, but every answer is a proposal. I accept it only after:
- tests that cover the risky paths (concurrency, time zones, ownership), not only the happy path;
- the official documentation for the exact versions in use;
- re-reading the original requirements.
