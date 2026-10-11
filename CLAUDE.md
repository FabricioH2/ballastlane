# Ballast Lane – Technical Interview Exercise (Pokédex API)

Full-stack exercise: a Spring Boot REST API that consumes PokeAPI and stores Pokémon locally with custom fields. It supports CRUD and user auth, and comes with a React + TypeScript frontend, Docker, a README and a GENAI.md.
Evaluation focus: **Clean Architecture, TDD, code quality**.

## Layout
- `backend/bla-excercise/`: Spring Boot 4.1, Java 25, Gradle (Kotlin DSL). Base package `com.ballastlane.blaexcercise`.
- `frontend/`: React 19 + TypeScript + Vite. Calls only `/api` on its own origin (Vite proxy in dev, nginx in Docker).
- `docs/diagrams/`: SVG diagrams (architecture, request flow, auth, data model, deployment, frontend). Update them when the structure changes.
- Docker: `compose.yaml` in the repo root; `Dockerfile` in `backend/bla-excercise/` and in `frontend/` (+ `frontend/nginx.conf`).

## Architecture rules (Clean / Hexagonal)
```
web/             controllers, request/response DTOs (web/model), GlobalExceptionHandler (web/errors)
application/
  port/in/       use case interfaces
  port/out/      interfaces the core needs (PokemonCatalog, LocalPokemonRepository, UserRepository, PasswordHasher, TokenIssuer)
  service/       use case implementations, plain Java
domain/
  model/         records: PokemonSummary, PokemonDetail, LocalPokemon, User, AuthToken, PageResult
  exception/     domain exceptions, mapped to HTTP codes in GlobalExceptionHandler
infra/
  pokeapi/       RestClient adapter implementing PokemonCatalog + PokeAPI DTOs + Caffeine cache
  persistence/   JPA entities + Spring Data + adapters (Flyway migrations in resources/db/migration)
  security/      SecurityConfig, JWT (oauth2 resource server, HS256), BCrypt
  config/        BeanConfig registers the application services as beans
```
- Dependencies point inward. `domain` and `application` must NOT import Spring, JPA or Jakarta.
- No `@Service` on application services; wire them in `infra/config/BeanConfig`.
- Use records for domain models and DTOs. JPA entities live only in `infra/persistence`.
- The schema belongs to Flyway (`ddl-auto: validate`). Never edit an applied migration; add `V<n>__...`.
- Demo data lives in `resources/db/seed` and is only loaded with the `demo` profile (Docker Compose turns it on).

## TDD workflow
Red → Green → Refactor. Make small Conventional Commits.
Tests:
- Use cases: JUnit 5 + Mockito + AssertJ.
- Controllers: `@WebMvcTest`.
- PokeAPI adapter: WireMock.
- Persistence: Testcontainers (PostgreSQL).
- End to end: `AuthFlowIntegrationTest`.

## Status
All user stories (US01–US04), CRUD, auth, frontend and Docker are done. PRESENTATION.md and diagrams are done. GENAI.md is done.
