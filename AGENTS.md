# AGENTS.md — Pokémon Catalog, Full-Stack

Instructions for any AI coding agent working in this repository. Read this file completely before
doing anything. It is the entry point. The detailed rules live in `docs/` and are binding.

---

## 1. What this project is

A **Spring Boot REST API** following **Clean Architecture** and **TDD**. It integrates **PokeAPI** (https://pokeapi.co/docs/v2) to
browse and look up Pokémon, replicates chosen Pokémon into **PostgreSQL**, and lets users enrich
them with proprietary fields. There's **user registration/authentication** with public vs protected
routes, plus a **React** frontend. It ships with a README, seed data and Docker.

The quality bar: Clean Architecture, test coverage through TDD, code quality, functionality
(including **no browser console warnings**), a clear walkthrough, and **critical use of GenAI**.
Every line has to survive code review, so correctness and clarity beat speed.

**The product, in short** (full version in
[`docs/domain-model.md` → Product vision](docs/domain-model.md#product-vision)):
- PokeAPI is the source. Only the backend talks to it.
- Anyone can browse the list and open a Pokémon's detail. The backend returns PokeAPI data merged
  with the local record in **one** response.
- A logged-in user can **sync** a Pokémon into the shared local database, then edit its proprietary
  fields (localized name, region, tags) or remove it, all on `/api/v1/pokemon/{identifier}/local`.
- Local data belongs to the organization, not to a user. Users exist to protect the writes.
- Two pages: list and detail (+ login/register).

The full requirement list, with IDs, is in [`docs/requirements.md`](docs/requirements.md). Treat
it as the contract.

## 2. How to talk to the developer

- **Everything that goes into the repository is in English**: code, identifiers, comments, docs,
  commit messages, UI copy, test names (D-025).
- Be direct. When something in a request conflicts with these rules or the brief, say so and
  propose the compliant alternative. Don't silently comply or silently deviate.

## 3. Document map (sources of truth)

| File | What it governs | When to read it |
|---|---|---|
| [`docs/requirements.md`](docs/requirements.md) | The brief as an ID'd checklist + evidence | Every task (pick the IDs you serve) |
| [`docs/plan.md`](docs/plan.md) | Phased backlog and "Current focus" | **Start and end of every session** |
| [`docs/decisions.md`](docs/decisions.md) | Why each choice was made (ADRs) | Before any design choice, and before adding a dependency |
| [`docs/domain-model.md`](docs/domain-model.md) | Ubiquitous language, aggregates, VOs, exceptions, use cases, API contract, PokeAPI mapping | Any backend or API work |
| [`docs/standards/backend.md`](docs/standards/backend.md) | Java/Spring rules: layers, naming, testing, ArchUnit, prohibitions | Any `backend/` work |
| [`docs/examples/`](docs/examples/) | Reference code for every pattern, **in this project's domain** | Before writing a new kind of class. Copy patterns from here, don't invent |
| [`docs/standards/frontend.md`](docs/standards/frontend.md) | React structure, state, UX, zero-console-warnings, tests | Any `frontend/` work |
| [`docs/standards/commits.md`](docs/standards/commits.md) | Conventional Commits + TDD rhythm | Every commit |
| [`docs/ai-log.md`](docs/ai-log.md) | Evidence of critical AI use | Append whenever an entry trigger occurs (§8) |
| [`docs/genai-case-study.md`](docs/genai-case-study.md) | The separate "task API via GenAI" case study | Plan Phase 7 |
| [`docs/walkthrough.md`](docs/walkthrough.md) | Walkthrough agenda, demo script, design FAQ | Plan Phase 8 |

**Precedence when they disagree:** the brief (`requirements.md`) > `decisions.md` (latest
Accepted entry) > `domain-model.md` > `standards/*` > `examples/*`. If you find a contradiction,
fix it in the same change, or raise it. Never pick one silently.

## 4. Session workflow

**At the start of a session**
1. Read `docs/plan.md` → "Current focus", and check whether any **Proposed** decision blocks the
   next task.
2. Run `git status` and `git log --oneline -10`, so you know the real state, not just the plan.
3. Tell the developer (in pt-BR) which task you're about to do and which requirement IDs it covers.
   Wait for confirmation if anything is ambiguous.

**For each task**
1. Identify the requirement IDs and the decision(s) involved. If the task needs a decision that
   doesn't exist yet, write it as **Proposed** in `decisions.md` and ask first.
2. Work **TDD, inward-out** (§6). Run the tests at every step and read the output. Don't assume.
3. Keep the change scoped to the task. If you spot something out of scope, note it in `plan.md`
   instead of fixing it on the side.
4. Run the full relevant check (§9) before calling the task done.
5. Update the docs in the **same change**: tick `plan.md`, add evidence to `requirements.md`, and
   update `domain-model.md` / the examples if the model or contract changed. Add an `ai-log.md`
   entry if a trigger occurred.
6. Propose a Conventional Commit message (`docs/standards/commits.md`). Commit only when the
   developer has asked you to commit.

**At the end of a session:** update "Current focus" in `plan.md`, so the next session (possibly
another agent) can pick up cold.

## 5. Guardrails — ask before you…

- **Add, remove or upgrade any dependency** (Gradle or npm). Write the decision as Proposed first.
- Delete files or directories, rewrite git history, force-push, or **push** at all.
- Change the **API contract** (paths, status codes, response shapes). Update `domain-model.md`
  first. A breaking change is a new `/api/v2` endpoint (D-029), not an edit.
- **Edit an existing Flyway migration.** Don't. Write a new one.
- Weaken a test, an ArchUnit rule, a lint rule, or a quality gate to make something pass.
- Deviate from a standard. If an exception is justified, document it as a decision.

**Never** commit secrets. The JWT secret and DB password come from env vars, with dev-only defaults
clearly marked. Never log passwords, tokens or user free text.

**Never invent external facts.** PokeAPI field names and units, Spring Boot 4 / Spring Framework 7
APIs and property names, Testcontainers coordinates, and library versions all change between
versions. Verify against the official docs, the resolved dependency tree
(`./gradlew dependencies`), the library source, or a real API response, and say what you checked.
When verification isn't possible, say so explicitly. A confident guess that turns out wrong is
exactly what code review should catch.

## 6. Engineering rules (universal — the stack-specific forms are in `docs/standards/`)

- **Strict Clean Architecture.** Dependencies point inward only. `domain` **and** `application`
  depend on nothing but `java.*` and inner layers: no `@Service`, `@Component`, `@Transactional`,
  Lombok, Jackson or slf4j in either. Use cases are **input-port interfaces** implemented by
  `*Interactor` classes, wired in the `UseCaseConfig` composition root. Everything outside is
  reached through output ports. ArchUnit enforces all of this (D-001…D-003).
- **Determinism.** Business logic never reads ambient state: no clock, no random, **no UUID
  generation**. `now` and new ids are produced at the edge (`interfaces/`) and passed down as
  parameters.
- **Never silently unwrap** an optional/nullable value. Handle absence explicitly: a fallback, an
  explicit throw, or a branch. `Optional.get()` and TS non-null `!` on real data are forbidden.
- **Never let invalid input silently no-op.** The owner of an invariant (VO constructor, factory,
  behaviour method) rejects it loudly. A guard that logs and returns is the same defect with an
  extra log line.
- **TDD: Red → Green → Refactor**, in that order, and confirm the red fails *for the right reason*.
  **One test at a time**: keep a list of behaviours to cover, but write a single failing test, make
  it pass with the minimum code, refactor, then pick the next. Never write the whole suite first.
  **Tests don't bend to the code.** A test changes only when the expected behaviour changes, the
  test itself was wrong, or its code needs a readability refactor, always in its own commit, never
  in the same step as production code. **One commit per step** (`test:` red → `feat:` green →
  `refactor:` if any), and never push while red (`docs/standards/commits.md`).
  **Start at the centre**: domain (plain JUnit, no mocks) → interactor (ports mocked, pure
  collaborators real) → adapters (real Postgres via Testcontainers, real HTTP stack via
  `@WebMvcTest`, recorded PokeAPI JSON). If you can't write the test first, stop. That usually
  means a design problem.
- **Errors are part of the contract.** One `ErrorResponse` shape, mapped by exception
  **category**, with domain messages written for the end user. The statuses are in
  `domain-model.md` → API contract.
- **Small, readable, boring code.** Explicit names from the naming tables. No cleverness that needs
  a comment to defend. No dead code, commented-out code, or TODOs without a plan item.
- **Few comments.** Comment only a non-obvious *why* (a trap, a constraint, a deliberate deviation),
  in one short line. Never restate what the code does. Longer rationale goes to `docs/`. This applies
  to every file: Java, TypeScript, Gradle, Dockerfiles, YAML, nginx.

## 7. Repository layout

```
.
├── AGENTS.md                 # this file (CLAUDE.md imports it)
├── README.md                 # setup, run, demo credentials, architecture overview (DL-1)
├── docker-compose.yml        # postgres + backend + frontend (DL-3)
├── docs/                     # requirements, plan, decisions, model, standards, examples, AI log
├── backend/                  # Spring Boot 4.1 · Java 25 · Gradle · base package dev.guilhermeds.backend
│   └── src/main/java/dev/guilhermeds/backend/{domain,application,infrastructure,interfaces}
├── frontend/                 # React 19 · TypeScript · Vite
│   └── src/{app,features/{pokemon,auth},shared,test}
└── genai-case-study/         # only if generated task-API code is kept (plan Phase 7). Isolated: not
                              # part of the backend build, never imported by the main project
```

## 8. AI collaboration log — your standing duty

Critical use of AI is part of the quality bar, so append an entry to `docs/ai-log.md` (template inside)
whenever:
- the developer corrects or rejects something you produced,
- a test, the compiler, ArchUnit or a doc check catches a defect in AI-written code,
- you catch your own mistake or abandon an approach,
- a risky external fact you'd have guessed turned out wrong on verification.

Be honest and specific. These entries feed the walkthrough.

## 9. Commands and Definition of Done

```bash
# backend (from backend/)
./gradlew test                 # unit tests + ArchUnit
./gradlew integrationTest      # *IT (Testcontainers — Docker must be running)
./gradlew check                # everything, incl. the merged coverage report
./gradlew bootRun              # needs Postgres: docker compose up -d postgres (host port 5433)

# frontend (from frontend/)
npm run lint && npm run typecheck && npm test -- --run && npm run build
npm run dev                    # Vite on :5173, proxies /api → :8080

# whole stack (from the root)
docker compose up --build
```

Some of these scripts and tasks are created in plan Phase 0/6. Until a command exists, don't claim
to have run it.

**A task is done only when:**
- [ ] Its tests were written first, and they pass, along with the whole suite. You ran it and read
      the output.
- [ ] `LayeredArchitectureTest` passes. No rule was weakened.
- [ ] Backend: every new endpoint has ITs for success **and** each documented error status
      (400 invalid + malformed, 401, 404, 409 as applicable).
- [ ] Frontend: lint, typecheck, tests and build are clean, with **zero console warnings**.
      Loading, empty and error states exist.
- [ ] No new dependency without an Accepted decision. No secrets. No sensitive logging.
- [ ] `plan.md` is ticked, `requirements.md` has evidence, and `domain-model.md`/examples are in
      sync. `ai-log.md` is updated if triggered.
- [ ] A Conventional Commit message is proposed, referencing requirement IDs.
