# DRAFT — prompt for the task management API (to be reviewed before it is run)

You are a senior Java engineer. Generate a complete, runnable RESTful API for a simple task
management system. Follow every constraint below; where something is ambiguous, choose the simplest
option and list the assumption at the end of your answer instead of asking.

## Stack

- Java 25, Spring Boot 4.1, Gradle (Groovy DSL), PostgreSQL 17, Flyway for the schema.
- Spring Web MVC, Spring Data JPA, Spring Security, Jakarta Bean Validation.
- Tests: JUnit 5, AssertJ, Mockito, `@WebMvcTest`, Testcontainers (PostgreSQL), ArchUnit.
- No Lombok. Constructor injection only.

## Architecture

Clean Architecture, in four packages under `com.example.tasks`:

- `domain`: the `Task` aggregate, value objects, domain exceptions, repository ports. Plain Java:
  no Spring, JPA, Jackson or Bean Validation annotations.
- `application`: one input-port interface per use case (`CreateTaskUseCase`, ...) implemented by an
  `*Interactor` class, plus input/output records. Plain Java as well; no `@Service`, no
  `@Transactional`. Transactions go through a `UnitOfWork` port.
- `infrastructure`: JPA entities and adapters for the ports, Spring Security, configuration, and the
  composition root that wires the interactors as beans.
- `interfaces`: REST controllers, request/response records, one global exception handler.

Dependencies point inward only. Include an ArchUnit test that enforces it.

## Model

- `Task`: `id` (UUID), `title`, `description`, `status`, `dueDate`, `ownerId`, `createdAt`,
  `updatedAt`, `version`.
- `TaskStatus`: `TODO`, `IN_PROGRESS`, `DONE`. Any status can move to any other.
- `User` already exists: `id` (UUID), `username` (unique), `passwordHash` (BCrypt). Seed two users
  in a Flyway migration, `alice` and `bob`, and give their plain passwords in the README.
- JSON uses camelCase (`dueDate`); the database column is `due_date`.

## Authentication and authorization

- HTTP Basic against the users table. Every `/api/v1/tasks` route requires authentication.
- The owner always comes from the authenticated user, never from the request body or a parameter.
- A user only sees and changes their own tasks. Another user's task answers **404**, exactly like a
  task that doesn't exist, so ids can't be probed.

## API (`/api/v1`)

| Method & path | Success | Notes |
|---|---|---|
| `GET /tasks?status=&page=0&size=20` | 200, a page of the caller's tasks | `status` optional filter; `size` 1..100 |
| `GET /tasks/{id}` | 200 | |
| `POST /tasks` | 201 + `Location` | `status` defaults to `TODO` |
| `PUT /tasks/{id}` | 200 | replaces title, description, status and dueDate; body carries `version` |
| `DELETE /tasks/{id}` | 204 | |

- Page response: `{ "content": [...], "page", "size", "totalElements", "totalPages" }`.
- Every error has one shape: `{ "code", "message", "fieldErrors": [ { "field", "message" } ] }`.
- 400: malformed JSON, invalid values, unknown status, bad paging parameters. 401: missing or wrong
  credentials. 404: missing or not owned. 409: `PUT` with a stale `version`. No stack traces or
  internal messages in any response.

## Validation (the domain owns the rules)

- `title`: required, trimmed, 1–200 characters.
- `description`: optional, at most 2000 characters; blank becomes null.
- `dueDate`: optional `LocalDate`; when given on create, or changed on update, it must not be before
  today in UTC. Read "today" from an injected `java.time.Clock`, never `LocalDate.now()` directly.
- The edge (Bean Validation) only checks shape; every rule lives in the domain and fails with a
  domain exception mapped to 400.

## Tests (write them as you would with TDD: behaviour first)

- Domain: plain JUnit, no mocks, every validation rule and its boundaries.
- Interactors: ports mocked with Mockito, including "another user's task is not found".
- Controllers: `@WebMvcTest` with security, for every success and every documented error status.
- Persistence and one end-to-end ownership test: Testcontainers PostgreSQL with the real migrations.
- The ArchUnit test above.

## Don'ts

No entities in responses; no `ddl-auto` other than `validate`; no field injection; no
`Optional.get()`; no catching `Exception` to hide errors; no logging of passwords or request bodies.

## Output

1. The file tree.
2. Every file, in full, each preceded by its path.
3. A README: how to run it (`docker compose up` for PostgreSQL, then Gradle), the seeded users, and
   `curl` examples.
4. The list of assumptions you made.
