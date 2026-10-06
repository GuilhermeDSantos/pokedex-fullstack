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
| OV-1 | RESTful API in **Java + Spring Boot** | ⬜ | |
| OV-2 | **Clean Architecture** (applied strictly — D-001) | ⬜ | `LayeredArchitectureTest` |
| OV-3 | **TDD** methodology, visible in history and tests | ⬜ | commit history (test-first), [`ai-log.md`](ai-log.md) |
| OV-4 | Backed by a reliable data store | ⬜ | PostgreSQL + Flyway |
| OV-5 | Integrates **PokeAPI** for retrieval, local replication and attribute modification | ⬜ | |
| OV-6 | The user stories are explicit in the docs and the walkthrough | ⬜ | [`walkthrough.md`](walkthrough.md) |

## Functional — user stories

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| FR-0 | Spring Boot REST API that **communicates with PokeAPI** | ⬜ | `PokemonSource` port → `PokeApiPokemonSource` |
| US-01 | **Browse** Pokémon with **paginated** results, each showing **sprite, category, mass, skills (abilities)** (D-010) | ⬜ | `GET /api/v1/pokemon` (merged with local data, D-030) |
| US-01.N | *Nice to have:* **cache** service responses | ⬜ | Caffeine on `PokeApiClient` (D-012) |
| US-02 | **Detail** of a chosen Pokémon: **image, core statistics, narrative description, evolutionary lineage** | ⬜ | `GET /api/v1/pokemon/{identifier}` (merged with local data) |
| US-03 | **Persist** Pokémon data into a **local relational store** (sync) | ⬜ | `POST /api/v1/pokemon/{identifier}/local` → `local_pokemons` (scalar snapshot, D-031) |
| US-03.a | Replication enables **proprietary fields**: localized nomenclature, geographical metadata, internal classification tags (D-006; one free-text `localizedName`, D-027 rejected) | ⬜ | `CustomAttributes` (localizedName, region, tags) — the brief's three examples |
| US-04 | **Update** any Pokémon in the local DB (editable fields = the proprietary ones: D-006/D-026) | ⬜ | `PUT /api/v1/pokemon/{identifier}/local` |
| US-04.a | **404** for missing records | ⬜ | |
| US-04.b | **400** for malformed payloads (malformed JSON **and** invalid values) | ⬜ | |
| US-04.c | Further **defensive logic** as required (409 concurrency/duplicates, size limits, input normalization, auth) | ⬜ | |

## Technical — mandatory

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| TR-GIT | Code hosted in a **public Git repository** | ⬜ | GitHub URL in README |
| TR-TEST | **Tests included** | ⬜ | |
| TR-ERR | **Proper error handling** (uniform `ErrorResponse`, category mapping) | 🟨 | `GlobalExceptionHandlerIT`: categories 404/409/400/401/422, malformed/invalid body and params → 400, unknown path 404, generic 500, framework 405/415 kept |
| TR-CACHE | *Nice to have:* **caching layer for PokeAPI** responses | ⬜ | same as US-01.N |
| TR-FE | **Front-end** consuming the API | ⬜ | `frontend/` |
| TR-OPT | *Optional:* additional functionality is welcome | ⬜ | e.g. OpenAPI UI (timeboxed, D-016), "synced" badge and display name on the list |

## Technical — database

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| TR-DB-1 | Relational DB with a **primary entity** (Pokémon) and a **secondary collection for user management** (user accounts) | 🟨 | `user_accounts` (`V1__…`), `local_pokemons` (`V2__…`) |
| TR-DB-2 | Records have a **unique primary key** and **≥ 2 descriptive attributes** | 🟨 | `local_pokemons.id`, `user_accounts.id` |

## Technical — API

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| TR-API-1 | Java Web API with **comprehensive CRUD** on the dataset | ⬜ | on `/api/v1/pokemon/{identifier}/local`: C = `POST` (sync), R = `GET`, U = `PUT`, D = `DELETE` |
| TR-API-2 | **Standard HTTP verbs**, required parameters, **consistent return structures** | ⬜ | `PageResponse`, `ErrorResponse`, statuses in [`domain-model.md`](domain-model.md#api-contract) |
| TR-AUTH-1 | Auxiliary API for **user registration** | 🟨 | `POST /api/v1/auth/register`: `AuthController`, `RegisterUserInteractor`; `RegisterUserInteractorTest`, `AuthControllerIT`, `AuthFlowIT` |
| TR-AUTH-2 | **Authentication** | 🟨 | `POST /api/v1/auth/login` (JWT). `AuthController` (login, `/auth/me`), `BCryptPasswordHasher`, `JwtTokenIssuer` + `JwtConfig` (HS256), `AuthenticateUserInteractor`, `GetCurrentUserInteractor`; their tests, `AuthControllerIT`, `AuthFlowIT` (real token end to end), `JwtTokenIssuerTest`, `JwtPropertiesTest` |
| TR-AUTH-3 | **Protected vs public routes** | 🟨 | `SecurityConfig`: every read public, writes on `/local` and `/auth/me` protected, closed by default (D-030, D-035); `SecurityConfigIT`. Controller-level 401 tests come with each endpoint |

## Technical — layers

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| TR-DAL | Specialized **data access layer** managing persistence interactions, the foundation for controllers | ⬜ | `infrastructure/persistence` behind domain repository ports |
| TR-BL-1 | Dedicated **business logic layer** with all domain rules and **data validation** (the domain is the validation authority: D-028) | ⬜ | `domain` + `application` |
| TR-BL-2 | Business layer **independent of both the API and data access** | ⬜ | framework-free `domain`/`application`, ArchUnit allowlists |
| TR-UT | **Thorough unit test coverage for every core component** | ⬜ | JaCoCo report (unit + IT data merged), test list per layer |

## Frontend

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| FE-1 | Modern framework (React) integrated with the backend | ⬜ | |
| FE-2 | **Responsive** and **user-centric** design | ⬜ | live resize in the demo (~360 px → desktop), four async states per view |
| FE-3 | **CRUD** matching the functional use cases | ⬜ | detail page: Sync (C), local section (R), Edit (U), Remove (D) |
| FE-4 | **Clean component organization** and **efficient state management** | ⬜ | [`standards/frontend.md`](standards/frontend.md) |
| FE-5 | *Optional but desired:* **no warnings in the browser console** | ⬜ | console guard in tests + manual pass |

## Submission & delivery

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| DL-1 | **README**: environment setup and technical documentation | ⬜ | `README.md` |
| DL-2 | **Pre-populated** with seeded data / mock credentials for the demo | ⬜ | `V3__seed_demo_data.sql`, credentials in README |
| DL-3 | **Dockerfile** for containerized execution | ⬜ | `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml` |

## AI-assisted development case study (separate deliverable)

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| AI-1 | The **prompt** to generate a Task Management REST API (CRUD; task has title, description, status, due_date; belongs to a user) | ⬜ | [`genai-case-study.md`](genai-case-study.md) |
| AI-2 | The **output code** (or a representative sample) | ⬜ | |
| AI-3 | How the AI's suggestions were **validated** | ⬜ | |
| AI-4 | How the output was **corrected / improved** | ⬜ | |
| AI-5 | How **edge cases, authentication and validations** were handled | ⬜ | |

## Quality criteria

| ID | Criterion | How we satisfy it |
|---|---|---|
| EV-1 | **Clean Architecture**: separation of concerns, component independence | Strict layering, input/output ports, composition root, ArchUnit |
| EV-2 | **Testing**: sufficient coverage, TDD preferred | Inward-out TDD, per-layer tests, coverage report, test-first commits |
| EV-3 | **Code quality**: organized, readable, best practices | Standards in `docs/standards/`, naming tables, prohibitions |
| EV-4 | **Functionality**: works per requirements without errors; no console warnings | This matrix all ✅, demo script, console guard |
| EV-5 | **Walkthrough**: clear, concise, shows backend + frontend best practices | [`walkthrough.md`](walkthrough.md) |
| EV-6 | **GenAI tools**: fluency, prompt engineering, critical review of AI code | [`ai-log.md`](ai-log.md), [`genai-case-study.md`](genai-case-study.md), this `AGENTS.md` setup itself |
