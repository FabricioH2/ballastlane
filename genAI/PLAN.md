The directory is empty, so the User model from the spec doesn't exist yet. Java 25.0.4 and Docker 29.8 are installed. Gradle is not.

## Assumptions

**Stack**
- **Spring Boot 4.0.x** (supports Java 25), Gradle Kotlin DSL, Testcontainers 2.x, base package `com.example.taskmanager`.
- **JWT** uses Spring's OAuth2 resource server (Nimbus) with an HS256 secret from config. I won't hand-write a token parser. The token's `sub` claim is the user's UUID.
- **IDs** are UUIDs. Timestamps are `Instant` values set by the domain from an injected `Clock`, so date rules can be tested.

**Architecture**
- `domain` and `application` import no framework code at all, not even Spring's paging types. `application` gets its own `PageQuery` and `PageResult`. Use cases are plain classes, wired up with `@Bean` in `infrastructure/config`.
- Ownership is enforced in the query itself (`findByIdAndOwnerId`). A task owned by someone else looks exactly like a missing one, so the caller gets 404.

**Behaviour**
- **PUT** replaces the whole task: title, description, status and dueDate. A missing title gets 400.
- **Due-date rule:**
  - On create, a past `dueDate` gets 400.
  - On update, you can keep an existing past `dueDate`, but you can't change it to a different past date.
  - The domain enforces this (with `Clock`), not `@FutureOrPresent`, which reads the system clock.
- **List:** default `page=0`, `size=20`, maximum size 100, sorted by `createdAt` descending. The response is a small DTO (`content`, `page`, `size`, `totalElements`, `totalPages`), not Spring's `Page`. An invalid `status` value gets 400.
- **Errors:** every error is a `ProblemDetail`, including 401, which goes through a custom `AuthenticationEntryPoint`. A 400 includes an `errors` array of `{field, message}`. Domain validation errors use the same format.

## Files

**Build and config:** `settings.gradle.kts`, `build.gradle.kts`, Gradle wrapper files, `.gitignore`, `docker-compose.yml` (local Postgres), `application.yml`, `db/migration/V1__create_users.sql`, `V2__create_tasks.sql`

**domain:** `task/Task.java`, `task/TaskStatus.java`, `user/User.java`, `DomainValidationException.java`

**application:**
- `port/out/TaskRepository.java`, `port/out/UserRepository.java`
- `common/PageQuery.java`, `common/PageResult.java`
- `task/CreateTaskUseCase.java` + `CreateTaskCommand.java`
- `task/UpdateTaskUseCase.java` + `UpdateTaskCommand.java`
- `task/GetTaskUseCase.java`, `ListTasksUseCase.java`, `DeleteTaskUseCase.java`
- `task/TaskNotFoundException.java`

**infrastructure:**
- `persistence/TaskJpaEntity.java`, `SpringDataTaskRepository.java`, `JpaTaskRepositoryAdapter.java`
- `persistence/UserJpaEntity.java`, `SpringDataUserRepository.java`, `JpaUserRepositoryAdapter.java`
- `security/SecurityConfig.java`, `JwtProperties.java`, `ProblemDetailAuthenticationEntryPoint.java`
- `config/UseCaseConfig.java`, `ClockConfig.java`

**web:** `TaskController.java`, `GlobalExceptionHandler.java`, `CurrentUser.java` (gets the owner UUID from the JWT), `dto/CreateTaskRequest.java`, `UpdateTaskRequest.java`, `TaskResponse.java`, `PageResponse.java`

**Tests (written first):**
- `domain/task/TaskTest.java`: domain rules (title length, past due date)
- `application/task/{Create,Get,List,Update,Delete}TaskUseCaseTest.java`: Mockito
- `web/TaskControllerTest.java`: `@WebMvcTest` covering 201 + Location, 400 with field errors, 401, 404, 204
- `infrastructure/persistence/JpaTaskRepositoryAdapterIT.java`: Testcontainers Postgres plus the Flyway migrations
- `TaskOwnershipIsolationIT.java`: full app with real JWTs. User B gets 404 on GET, PUT and DELETE of user A's task, and the test checks A's task is unchanged afterwards.
- `testsupport/PostgresTestcontainer.java` (`@ServiceConnection`), `testsupport/JwtTestTokens.java`

## Questions before I start

1. **How do clients get a JWT?** The spec doesn't cover it.
   - **(a) Default:** a public `POST /api/auth/login` that checks the password against `passwordHash` (BCrypt) and issues a token. That adds an `AuthenticateUserUseCase`, a `PasswordVerifier` and a `TokenIssuer` port, their adapters, `AuthController` and its DTOs, and one unit test.
   - **(b)** Tokens come from somewhere else, and this API only validates them.
2. **Gradle wrapper:** Gradle isn't installed, and the wrapper needs a binary `gradle-wrapper.jar`.
   - **Default:** with your OK, I download the Gradle 9.x distribution from `services.gradle.org` (about 130 MB) into the scratchpad and run `gradle wrapper`. Then I can also run the tests.
   - Otherwise, I write the wrapper properties and scripts only, and you generate the jar yourself.
3. **Optional:** an ArchUnit test that fails the build if `domain` or `application` imports Spring, JPA or Jakarta code, so the dependency rule is checked automatically. Default: yes.
4. Is Spring Boot 4.0.x OK, or do you need 3.5.x?

Reply "go" to accept all the defaults, or tell me what to change.
