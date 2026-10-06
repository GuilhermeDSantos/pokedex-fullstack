# Pokémon Catalog

A Pokémon catalog service backed by [PokeAPI](https://pokeapi.co/). Anyone can browse Pokémon and
open their details. Signed-in users can sync a Pokémon into the local database and enrich it with
the organization's own data: a localized name, a region and classification tags.

- **Backend:** Java 25 · Spring Boot 4 · Clean Architecture · PostgreSQL · Flyway
- **Frontend:** React 19 · TypeScript · Vite

> This README covers how to run the project. Architecture, API and design decisions are in
> [`docs/`](docs/), starting with [`docs/domain-model.md`](docs/domain-model.md).

## Run everything with Docker

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| Web app | http://localhost:3000 |
| API | http://localhost:8080 (health: `/actuator/health`) |
| API docs (Swagger UI) | http://localhost:8080/swagger-ui.html: sign in with `POST /auth/login`, then **Authorize** with the `accessToken` to try the protected routes |
| PostgreSQL | `localhost:5433`, database/user `pokedex` |

The defaults are for local use only. To override them, copy `.env.example` to `.env`.

## Run locally (development)

Requirements: Docker (for PostgreSQL and the integration tests), Node 24. The JDK 25 toolchain is
downloaded by Gradle if it isn't installed.

```bash
docker compose up -d postgres          # database only, on localhost:5433

cd backend
./gradlew bootRun                      # API on :8080

cd frontend
npm install
npm run dev                            # app on :5173, /api proxied to :8080
```

## Tests

```bash
cd backend
./gradlew test                         # unit tests
./gradlew integrationTest              # integration tests (Testcontainers, needs Docker)
./gradlew check                        # everything + coverage report (build/reports/jacoco)
```
