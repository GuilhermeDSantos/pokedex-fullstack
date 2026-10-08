# Task API

The task management API of the [GenAI case study](../../docs/genai-case-study.md): authenticated
users create, read, update and delete their own tasks. Java 25 · Spring Boot 4.1 · PostgreSQL 17 ·
Flyway · HTTP Basic. It is isolated from the Pokémon project's build.

## Run it

Requirements: Docker and a JDK 25 (or let Gradle download the toolchain).

```bash
docker compose up -d          # PostgreSQL on localhost:5434; start it before the app
./gradlew bootRun             # API on http://localhost:8081
```

The ports (8081 and 5434) let it run beside the Pokémon project. To stop and delete the database:
`docker compose down -v`.

## Users

Seeded by the `V3__seed_users` migration. There's no registration endpoint: the `User` model is
assumed to exist.

| Username | Password |
|---|---|
| `alice` | `alice-pass-1` |
| `bob` | `bob-pass-1` |

## Try it

```bash
# Create (201 + Location); the owner is whoever signs in
curl -i -u alice:alice-pass-1 -H 'Content-Type: application/json' \
     -d '{"title":"Buy milk","dueDate":"2099-01-01"}' localhost:8081/api/v1/tasks

# List Alice's tasks, optionally by status, a page at a time (size 1..100)
curl -u alice:alice-pass-1 'localhost:8081/api/v1/tasks?status=TODO&page=0&size=20'

# Read, update (send the version you read; a stale one is a 409), delete
curl -u alice:alice-pass-1 localhost:8081/api/v1/tasks/{id}
curl -u alice:alice-pass-1 -X PUT -H 'Content-Type: application/json' \
     -d '{"title":"Buy oat milk","status":"DONE","version":0}' localhost:8081/api/v1/tasks/{id}
curl -u alice:alice-pass-1 -X DELETE localhost:8081/api/v1/tasks/{id}

# Bob can't see, change or delete Alice's task: 404, as if it didn't exist
curl -u bob:bob-pass-1 localhost:8081/api/v1/tasks/{id}
```

## Tests

```bash
./gradlew test                # domain, use cases, ArchUnit
./gradlew check               # plus the integration tests (Testcontainers, needs Docker)
```
