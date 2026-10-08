# Requirements — Traceability Matrix

The project brief in checklist form. Each requirement
has a stable ID, and every commit, test and plan item refers to these IDs. The **Evidence** column
is filled in as the work lands: test class names, endpoints, files. That makes the matrix the
backbone of the walkthrough ("here is each requirement, and here is the proof").

Status: ⬜ not started · 🟨 in progress · ✅ done (with evidence) · ➖ deliberately out of scope (see decisions)

The brief's own wording is paraphrased. Where it's ambiguous, the interpretation we chose is linked
to a decision in [`decisions.md`](decisions.md).

---

## Project overview (applies to everything)

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| OV-1 | RESTful API in **Java + Spring Boot** | ✅ | Spring Boot 4.1 on Java 25, REST under `/api/v1` ([API contract](domain-model.md#api-contract)); every endpoint has success and error ITs |
| OV-2 | **Clean Architecture** (applied strictly — D-001) | ✅ | `domain` and `application` framework-free, ports and adapters, `UseCaseConfig` as the composition root; `LayeredArchitectureTest` (17 rules) fails the build on any violation (D-001…D-003) |
| OV-3 | **TDD** methodology, visible in history and tests | ✅ | Inward-out TDD visible in the history: 305 `test:` commits (red) and 239 `feat:` commits (green), plus `refactor:` steps; [`ai-log.md`](ai-log.md) |
| OV-4 | Backed by a reliable data store | ✅ | PostgreSQL 17 with Flyway-owned migrations (`ddl-auto: validate`); unreachable database → 503 within 3 s (`DatabaseUnavailableIT`, `DatabaseConnectionLostIT`) |
| OV-5 | Integrates **PokeAPI** for retrieval, local replication and attribute modification | ✅ | Retrieval (list and detail from PokeAPI), local replication (sync, `POST …/local`), attribute modification (`PUT …/local`); `PokemonControllerIT`, `JpaLocalPokemonRepositoryIT`, the demo on Docker |
| OV-6 | The user stories are explicit in the docs and the walkthrough | ✅ | US-01…US-04 in this matrix and in [`domain-model.md`](domain-model.md#product-vision); [`walkthrough.md`](walkthrough.md#user-stories--what-to-show) maps each story to its API, screen and demo step |

## Functional — user stories

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| FR-0 | Spring Boot REST API that **communicates with PokeAPI** | ✅ | `PokemonRepository` port → `PokeApiPokemonRepository` + `PokeApiClient` (RestClient, timeouts, cache) and `PokeApiTranslator`; `PokeApiClientTest`, `PokeApiClientTimeoutIT`, `PokeApiTranslatorTest`, `PokeApiPokemonRepositoryTest` |
| US-01 | **Browse** Pokémon with **paginated** results, each showing **sprite, category, mass, skills (abilities)** (D-010) | ✅ | `GET /api/v1/pokemon` (`BrowsePokemonInteractorTest`, `PokemonControllerIT`); `PokemonListPage` (`PokemonListPage.test.tsx`, `PokemonCard.test.tsx`); each card carries our localized name, from one query per page (`JpaLocalPokemonRepositoryIT` counts it, D-030); checked on Docker |
| US-01.N | *Nice to have:* **cache** service responses | ✅ | Caffeine on `PokeApiClient` (D-012): `PokeApiClientCacheTest`, `PokeApiPokemonRepositoryCacheTest`; a cached list page answers in milliseconds |
| US-02 | **Detail** of a chosen Pokémon: **image, core statistics, narrative description, evolutionary lineage** | ✅ | `GET /api/v1/pokemon/{identifier}`: image, stats, description, evolution tree (branching, e.g. Eevee), merged with our record (`GetPokemonInteractorTest`, `PokemonControllerIT`, `PokeApiTranslatorTest`); `PokemonDetailPage` (`PokemonDetailPage.test.tsx`) |
| US-03 | **Persist** Pokémon data into a **local relational store** (sync) | ✅ | `POST /api/v1/pokemon/{identifier}/local` → 201 + `Location`, a row in `local_pokemons` (Pokédex number + our fields, D-039). `JpaLocalPokemonRepositoryIT`, `PokemonControllerIT` (201/400/401/404/409/503), `SyncPokemonInteractorTest`; the detail page's **Sync to local database** checked on Docker |
| US-03.a | Replication enables **proprietary fields**: localized nomenclature, geographical metadata, internal classification tags (D-006; one free-text `localizedName`, D-027 rejected) | ✅ | `CustomAttributes` (localizedName, region, tags), the brief's three examples: validated in the domain (`CustomAttributesTest`, `TagTest`), stored in `local_pokemons` + `local_pokemon_tags`, edited with `PUT …/local`, shown on the detail (name, facts, tags) and on the list (localized name) |
| US-04 | **Update** any Pokémon in the local DB (editable fields = the proprietary ones: D-006/D-026) | ✅ | `PUT /api/v1/pokemon/{number}/local` (D-040) replaces our fields: `UpdateLocalPokemonInteractorTest`, `LocalPokemonTest`, `JpaLocalPokemonRepositoryIT` (repeated edits), `PokemonControllerIT`; checked with curl on Docker |
| US-04.a | **404** for missing records | ✅ | `LocalPokemonNotFoundException` → 404 on `GET`/`PUT`/`DELETE …/local` (`PokemonControllerIT`); a removal of what was never synced is 404, not 204 |
| US-04.b | **400** for malformed payloads (malformed JSON **and** invalid values) | ✅ | Malformed JSON → "Malformed JSON request body" (never echoed); sizes → `fieldErrors` per field; tag format and a name instead of a number → domain `ValidationException` (`PokemonControllerIT`, `TagTest`, `CustomAttributesTest`, `PokedexNumberTest`) |
| US-04.c | Further **defensive logic** as required (409 concurrency/duplicates, size limits, input normalization, auth) | ✅ | 409 on a concurrent edit (`@Version`, two-transaction IT) and on a second sync; size limits at the edge and in the domain; tags trimmed, lower-cased, deduplicated; texts trimmed, blank = not set; writes need a token (401) |

## Technical — mandatory

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| TR-GIT | Code hosted in a **public Git repository** | ✅ | https://github.com/GuilhermeDSantos/pokedex-fullstack (public) |
| TR-TEST | **Tests included** | ✅ | Backend: 181 unit tests, 108 integration tests (Testcontainers), 17 ArchUnit rules; frontend: Vitest + Testing Library + MSW, failing on any console output |
| TR-ERR | **Proper error handling** (uniform `ErrorResponse`, category mapping) | ✅ | One `ErrorResponse` shape mapped by exception category; `GlobalExceptionHandlerIT`: 404/409/400/401/422, malformed and invalid bodies → 400, unknown path 404, generic 500 without internals, framework 405/415 kept; unreachable data → 503 `DATA_UNAVAILABLE` |
| TR-CACHE | *Nice to have:* **caching layer for PokeAPI** responses | ✅ | Same as US-01.N |
| TR-FE | **Front-end** consuming the API | ⬜ | `frontend/` |
| TR-OPT | *Optional:* additional functionality is welcome | ✅ | Swagger UI at `/swagger-ui.html` with bearer auth (D-016, `OpenApiIT`); localized names on the list; demo data; the 503 for unavailable data |

## Technical — database

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| TR-DB-1 | Relational DB with a **primary entity** (Pokémon) and a **secondary collection for user management** (user accounts) | ✅ | `local_pokemons` (+ `local_pokemon_tags`) as the primary entity, `user_accounts` for user management (`V1__…`, `V2__…`); `JpaLocalPokemonRepositoryIT`, `JpaUserAccountRepositoryIT` |
| TR-DB-2 | Records have a **unique primary key** and **≥ 2 descriptive attributes** | ✅ | UUID primary keys; `local_pokemons`: Pokédex number (unique), localized name, region, tags, timestamps; `user_accounts`: email (unique), name, password hash, created at |

## Technical — API

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| TR-API-1 | Java Web API with **comprehensive CRUD** on the dataset | ✅ | on `/api/v1/pokemon/{number}/local`: C = `POST` (sync), R = `GET`, U = `PUT`, D = `DELETE`, each with ITs for success and every documented error |
| TR-API-2 | **Standard HTTP verbs**, required parameters, **consistent return structures** | ✅ | GET/POST/PUT/DELETE with their standard statuses (200/201 + `Location`/204), path and query parameters validated (400), `PageResponse` and `ErrorResponse` everywhere; statuses in [`domain-model.md`](domain-model.md#api-contract) |
| TR-AUTH-1 | Auxiliary API for **user registration** | ✅ | `POST /api/v1/auth/register`: `AuthController`, `RegisterUserInteractor`; `RegisterUserInteractorTest`, `AuthControllerIT`, `AuthFlowIT` |
| TR-AUTH-2 | **Authentication** | ✅ | `POST /api/v1/auth/login` (JWT HS256, BCrypt): `AuthenticateUserInteractor`, `GetCurrentUserInteractor`, `JwtTokenIssuer`; `AuthControllerIT`, `AuthFlowIT` (a real token end to end), `JwtTokenIssuerTest` |
| TR-AUTH-3 | **Protected vs public routes** | ✅ | `SecurityConfig`: every read public, writes on `/local` and `/auth/me` protected, closed by default (D-030, D-035); public routes ignore the token (D-036); `SecurityConfigIT` and a 401 IT for each protected endpoint |

## Technical — layers

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| TR-DAL | Specialized **data access layer** managing persistence interactions, the foundation for controllers | ✅ | `infrastructure/persistence`: JPA entities, Spring Data repositories and adapters behind the domain's repository ports, with exception translation; `JpaLocalPokemonRepositoryIT`, `JpaUserAccountRepositoryIT` |
| TR-BL-1 | Dedicated **business logic layer** with all domain rules and **data validation** (the domain is the validation authority: D-028) | ✅ | `domain` holds the rules and the validation (value objects such as `Tag`, `CustomAttributes`, `Email`, `RawPassword`, `PokedexNumber`); `application` orchestrates them in use cases (D-028) |
| TR-BL-2 | Business layer **independent of both the API and data access** | ✅ | `domain` and `application` import only `java.*` and inner layers, enforced by the ArchUnit allowlists in `LayeredArchitectureTest` |
| TR-UT | **Thorough unit test coverage for every core component** | ✅ | JaCoCo over unit + integration tests: **97.7% of lines, 82.1% of branches** in the backend; unit tests per layer (domain plain JUnit, interactors with mocked ports) |

## Frontend

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| FE-1 | Modern framework (React) integrated with the backend | 🟨 | React 19 + Vite, React Router, TanStack Query; sign up / sign in / sign out against the real API (S1.10) |
| FE-2 | **Responsive** and **user-centric** design | 🟨 | live resize in the demo (~360 px → desktop), four async states per view |
| FE-3 | **CRUD** matching the functional use cases | ✅ | detail page: Sync (C), our fields merged into the page (R), Edit in a dialog (U), Remove after a `ConfirmDialog` (D). `PokemonDetailPage.test.tsx`; checked in the browser on Docker |
| FE-4 | **Clean component organization** and **efficient state management** | ⬜ | [`standards/frontend.md`](standards/frontend.md) |
| FE-5 | *Optional but desired:* **no warnings in the browser console** | ✅ | console guard in `src/test/setup.ts` (fails any test that writes to the console); manual pass (D.6) on every page: list, detail, sign in, sign up, unknown route, unknown Pokémon. The only lines are the browser's own network log for a deliberate 4xx (an unknown Pokémon, a rejected edit), which no page code writes |

## Submission & delivery

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| DL-1 | **README**: environment setup and technical documentation | ✅ | `README.md`: quick start, demo account and script, architecture and data model diagrams, API table, decisions summary, tests, local development, limitations, how AI was used |
| DL-2 | **Pre-populated** with seeded data / mock credentials for the demo | ✅ | `V3__seed_demo_data.sql`: the demo account (hash from the app's own `BCryptPasswordHasher`) and ten synced Pokémon, numbers and French names from PokeAPI, idempotent on an existing database; credentials in the README; `ApplicationContextIT` signs the demo account in and checks the ten records |
| DL-3 | **Dockerfile** for containerized execution | ✅ | `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml`; a clean `docker compose up --build` on empty volumes (a separate Compose project) ran the whole demo (D.3) |

## AI-assisted development case study (separate deliverable)

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| AI-1 | The **prompt** to generate a Task Management REST API (CRUD; task has title, description, status, due_date; belongs to a user) | ✅ | [`genai-case-study/PROMPT.md`](../genai-case-study/PROMPT.md), verbatim; its design in [`genai-case-study.md`](genai-case-study.md) §2 |
| AI-2 | The **output code** (or a representative sample) | ✅ | [`genai-case-study/task-api/`](../genai-case-study/task-api/), isolated from the main build; tree and key files in §3 |
| AI-3 | How the AI's suggestions were **validated** | ✅ | §4: `./gradlew check` (34 unit, 19 integration tests), the main project's ArchUnit rules, `curl` against the running app |
| AI-4 | How the output was **corrected / improved** | ✅ | §5: five real defects with the fix and how it was caught, from a blind first run to a mock that would have returned `null` |
| AI-5 | How **edge cases, authentication and validations** were handled | ✅ | §6: ownership as a 404 in every operation (`TaskOwnershipIT`), HTTP Basic, domain-owned validation, the UTC "today" limitation |

## Quality criteria

| ID | Criterion | How we satisfy it |
|---|---|---|
| EV-1 | **Clean Architecture**: separation of concerns, component independence | Strict layering, input/output ports, composition root, ArchUnit |
| EV-2 | **Testing**: sufficient coverage, TDD preferred | Inward-out TDD, per-layer tests, coverage report, test-first commits |
| EV-3 | **Code quality**: organized, readable, best practices | Standards in `docs/standards/`, naming tables, prohibitions |
| EV-4 | **Functionality**: works per requirements without errors; no console warnings | This matrix all ✅, demo script, console guard |
| EV-5 | **Walkthrough**: clear, concise, shows backend + frontend best practices | [`walkthrough.md`](walkthrough.md) |
| EV-6 | **GenAI tools**: fluency, prompt engineering, critical review of AI code | [`ai-log.md`](ai-log.md), [`genai-case-study.md`](genai-case-study.md), this `AGENTS.md` setup itself |
