# Execution Plan

The living backlog. **The agent reads this at the start of every session and updates it at the end
of every task.** It ticks the box, adds evidence to [`requirements.md`](requirements.md), and moves
"Current focus".

Each task lists the requirement IDs it serves. For backend features the order inside a task is
always TDD inward-out: domain → interactor → adapters. The product being built is described in
[`domain-model.md` → Product vision](domain-model.md#product-vision).

---

## Current focus

> **Phase 1 — Backend foundation.** Next task: 1.2.
> Phase 0 is done: the whole stack runs with `docker compose up --build` and `./gradlew check` is
> green. No blockers. The agent still asks before every push.

---

## Priorities

**The brief comes first.** Anything not needed to satisfy a row in `requirements.md` is in
"Parked" and stays there until every phase below is done.

If scope has to shrink, cut in this order (each step keeps every requirement covered):
1. Springdoc (5.4). The README `curl` examples cover it.
2. `GET /pokemon/{identifier}/local` in the frontend: the detail response already carries `local`.
   The endpoint stays in the API.
3. Frontend tests trimmed to one happy path + one error path per page (the console guard stays).

Never cut: TDD on domain and interactors, the ArchUnit test, the error-status ITs, the console
guard, Docker, the README, the GenAI case study.

---

## Phase 0 — Repository & tooling setup

- [x] 0.1 Monorepo hygiene (D-017): `backend/.git` removed (it had no commits), root `.gitignore`
      added (OS, IDE, build output, `.env`). `HELP.md`, `*.iml` and `.idea/` were never tracked:
      `backend/.gitignore` already excludes them, so nothing had to be deleted. The agent docs
      (`AGENTS.md`, `CLAUDE.md`, `docs/`) are versioned on purpose.
- [x] 0.2 Build reproducible on JDK 25: Foojay resolver 1.0.0 in `settings.gradle` (it
      auto-provisioned Temurin 25; the machine's default JDK is 26). `./gradlew build` green.
- [x] 0.3 `build.gradle`: data-jpa, cache + Caffeine (D-012), oauth2-resource-server (D-008), the
      test deps (D-015: Testcontainers 2.0.5, ArchUnit 1.5.1, JaCoCo), Lombok removed (D-014), plain
      jar disabled, `bootJar` → `app.jar`. `integrationTest` task wired into `check`, and JaCoCo
      reading both `test` and `integrationTest`. Proven: `ApplicationHealthIT` was made to fail on
      purpose and the build failed. **Temporary:** `failOnNoMatchingTests`/`failOnNoDiscoveredTests`
      are off on `test` until 1.1 adds the first unit test.
- [x] 0.4 `application.yaml` baseline: datasource from env with local defaults, `ddl-auto: validate`,
      `open-in-view: false`, virtual threads on, `default_batch_fetch_size`, only `health` exposed.
      `pokeapi.*` and `security.jwt.*` are added later, together with the classes that bind them.
      No `app.cors.*`: everything is same-origin (D-019).
- [x] 0.5 `docker-compose.yml` (D-019): `postgres` (host port 5433), `backend` and `frontend`
      (nginx, `/api` proxied), each with a healthcheck and ordered startup; `backend/Dockerfile`,
      `frontend/Dockerfile` + `nginx.conf`, `.dockerignore`s, `.env.example`, Vite dev proxy.
      Verified from scratch (`down -v` → `up --build`): all three healthy, `/actuator/health` UP,
      SPA fallback 200, `/api` reaches Spring through nginx and through Vite, other actuator
      endpoints hidden, backend runs as non-root, empty browser console. `bootRun` against the
      compose Postgres: UP in ~6 s.
- [x] 0.6 `main` pushed to `github.com/GuilhermeDSantos/pokedex-fullstack` (TR-GIT). The repo
      must be **public** before delivery: confirm the visibility on GitHub (`gh` isn't installed
      locally, so it wasn't checked from here).

## Phase 1 — Backend foundation (OV-2, TR-ERR, TR-API-2)

- [x] 1.1 `LayeredArchitectureTest`: 17 ArchUnit rules from
      [`examples/tests.md`](examples/tests.md#architecture-test), green on the empty skeleton.
      Proven with a throwaway domain class (Spring annotation + `Instant.now()` + public
      constructor): exactly those three rules failed. The temporary `failOn…` lines in
      `build.gradle` are removed. **Temporary:** `archunit.properties` allows empty rules until
      every layer has classes (removed in 3.5).
- [ ] 1.2 Domain kernel: `DomainException` + the 4 categories, `domain/pagination` (`PageRequest`,
      `Page`) with tests.
- [ ] 1.3 `UnitOfWork` port + `SpringUnitOfWork`, `ClockConfig`, an empty `UseCaseConfig`.
- [ ] 1.4 `interfaces/rest`: `ErrorResponse`, `PageResponse`, `GlobalExceptionHandler` covering all
      categories + framework exceptions, with `GlobalExceptionHandlerIT`.
- [ ] 1.5 `SecurityConfig` skeleton (stateless, CSRF off, no CORS) with everything permitted except a
      test route, so the IT proves the 401 shape. The JSON 401/403 writers live in
      `interfaces/rest/security/` and reach `SecurityConfig` through Spring Security's interfaces
      (`infrastructure_must_not_depend_on_interfaces`).

## Phase 2 — PokeAPI integration (FR-0, US-01.N, TR-CACHE)

- [ ] 2.1 Domain VOs (TDD): `PokedexNumber`, `PokemonIdentifier`, `Height`, `Weight`, `PokemonType`,
      `Ability`, `BaseStat`/`StatName`, `PokemonSnapshot`, `PokemonProfile` (+ `toSnapshot()`).
      Source types: `PokemonSummary`, `PokemonDetail`, `EvolutionStage`, plus the `PokemonSource`
      port, `PokemonSourceUnavailableException` and `PokemonNotFoundException`.
- [ ] 2.2 Record PokeAPI fixtures (`curl`, commit them under `src/test/resources/pokeapi/`):
      bulbasaur (1), pikachu (25), eevee (133, branching chain), plus a list page. No alternate
      form: forms are a known limitation (Parked).
- [ ] 2.3 `PokeApiTranslator` + `PokeApiTranslatorTest` (units, genus, flavor-text normalization,
      evolution tree, null sprites).
- [ ] 2.4 `PokeApiClient` (`RestClient`, timeouts, 404 → empty, failures →
      `PokemonSourceUnavailableException`) + `@RestClientTest`. Add `spring-boot-starter-restclient`
      and `spring-boot-starter-restclient-test` first (Boot 4 split them out; see backend.md).
- [ ] 2.5 `PokeApiPokemonSource` on top of it (translation, concurrent fan-out on virtual threads).
- [ ] 2.6 Caching (D-012) on `PokeApiClient` only, + a test that a second
      `PokemonSource.getByIdentifier` call makes no HTTP request (guards the self-invocation trap).

## Phase 3 — Pokémon endpoints: merged reads + local data (US-01…US-04, US-03.a, TR-API-1, TR-DAL)

- [ ] 3.1 Domain (TDD): `LocalPokemonId`, `Tag`, `CustomAttributes`, the `LocalPokemon` aggregate
      (`create`, `updateCustomAttributes`, `displayName`) + `LocalPokemonNotFoundException`,
      `PokemonAlreadySyncedException`, `LocalPokemonModifiedConcurrentlyException` and the
      validation exceptions.
- [ ] 3.2 Migration `V1__create_local_pokemons.sql` (`local_pokemons` + `local_pokemon_tags`).
- [ ] 3.3 `LocalPokemonEntity`, `LocalPokemonJpaRepository`, `LocalPokemonEntityMapper`,
      `JpaLocalPokemonRepository` + `JpaLocalPokemonRepositoryIT` (whole-aggregate round trip, find
      by name and by number, `findAllByPokedexNumbers`, unique constraint → 409, optimistic lock →
      409).
- [ ] 3.4 Interactors (TDD): `BrowsePokemon` and `GetPokemon` (the merge), `SyncPokemon` (no
      transaction during the PokeAPI call), `GetLocalPokemon`, `UpdateLocalPokemon`,
      `RemoveLocalPokemon`.
- [ ] 3.5 `PokemonController` + `PokemonControllerIT`: every status in the contract, including
      reads without a token (public), writes without a token → 401, malformed JSON → 400, not
      synced → 404, already synced → 409. Then delete `src/test/resources/archunit.properties`:
      every layer now has classes, so an ArchUnit rule that matches nothing must fail again.

## Phase 4 — Users & authentication (TR-DB-1, TR-AUTH-1..3)

- [ ] 4.1 Domain: `UserId`, `Email`, `DisplayName`, `RawPassword` (redacted), `PasswordHash`,
      `UserAccount` + tests.
- [ ] 4.2 Migration `V2__create_user_accounts.sql`, then the persistence adapter + IT (unique email → 409).
- [ ] 4.3 Ports `PasswordHasher`, `TokenIssuer`, then the adapters `BCryptPasswordHasher`,
      `JwtTokenIssuer` + tests.
- [ ] 4.4 Interactors: Register, Authenticate (same 401 for unknown email and wrong password),
      GetCurrentUser.
- [ ] 4.5 `AuthController` + IT. Final route policy in `SecurityConfig` (D-030), with 401 tests on
      every protected endpoint.

## Phase 5 — Delivery: seed, Docker, docs (DL-1..3, TR-OPT)

- [ ] 5.1 `V3__seed_demo_data.sql`: demo user (BCrypt hash) + about 10 synced Pokémon with custom
      attributes and tags, so the list starts with merged data. The snapshot values come from real
      PokeAPI responses recorded with `curl` (like the 2.2 fixtures), never typed from memory.
      **Pikachu is not in the seed**: it's the Pokémon synced live in the demo (a 201, not a 409)
      and the test fixture.
- [ ] 5.2 `ApplicationContextIT`: context loads, every `*UseCase` bean resolves, seed present.
- [ ] 5.3 Containers were done in 0.5. Here: add the JWT secret env var to compose and
      `.env.example`, then verify a clean `docker compose up --build` from scratch again, with the
      seed, before delivery.
- [ ] 5.4 OpenAPI UI via springdoc, **timeboxed to 30 minutes** (D-016). If it doesn't work on the
      first try, remove the dependency and move on.
- [ ] 5.5 `README.md`: product overview (from the domain model's product vision), architecture
      diagram, how to run (Docker, and locally), demo credentials, API table, testing commands,
      design decisions summary (linking `docs/decisions.md`), known limitations and next steps, and
      how AI was used.

## Phase 6 — Frontend (TR-FE, FE-1..5)

- [ ] 6.1 Setup (D-020, D-021, D-023): router, query client, Vitest + RTL + MSW, the console-guard
      setup, the `/api` dev proxy, a `typecheck` script, design tokens, and the `AppShell` layout.
- [ ] 6.2 `shared/api/httpClient` (base path, auth header only on protected calls, `ApiError`, 401
      handling) + tests.
- [ ] 6.3 `shared/ui` primitives: Button, Input, Card, Skeleton, Pagination, EmptyState, ErrorState,
      ConfirmDialog.
- [ ] 6.4 **List page** (US-01): every card shows the brief's four fields (sprite, category, mass in
      kg, skills = abilities), plus the display name, number and a "synced" badge. Responsive grid,
      URL pagination, skeletons, error/empty states.
- [ ] 6.5 **Detail page** (US-02): artwork, display name (original name underneath when different),
      stats, description, evolution tree (branching).
- [ ] 6.6 Auth: register, login, logout, session restore on reload, "Log in to sync" with `returnTo`.
- [ ] 6.7 **Local section of the detail page** (US-03, US-04): Sync button when `local` is null
      (409 → refetch + inline note); localized name, region and tags with an inline Edit form
      (controlled inputs, D-022, server field errors mapped) and Remove with confirmation, when
      synced.
- [ ] 6.8 Quality pass: zero console warnings on both pages (manual, DevTools open), keyboard-only
      run, a manual check at ~360 px and desktop width (shown live in the demo), `lint` +
      `typecheck` + `test` + `build` all clean.

## Phase 7 — GenAI case study (AI-1..5)

Independent of the Pokémon code. Do it as soon as the backend phases are done, before the frontend.

- [ ] 7.1 Write the prompt in [`genai-case-study.md`](genai-case-study.md), run it, and save the raw output.
- [ ] 7.2 Review the output against a checklist, then record the defects found and the fixes, with
      before/after snippets.
- [ ] 7.3 Write up edge cases, auth and validation handling, plus lessons learned.

## Phase 8 — Walkthrough (EV-1..6)

- [ ] 8.1 Fill in the [`walkthrough.md`](walkthrough.md) agenda and demo script, and have the
      evidence for every row of `requirements.md` ready.
- [ ] 8.2 Check the answers in the design FAQ against the final code, with a full dry run from
      `docker compose up`.
- [ ] 8.3 Final check: every row in `requirements.md` is ✅ with evidence, the repo is public, the
      README is accurate, and CI is green (if added).

## Parked (ideas worth keeping; build only if every phase above is done)

Product and domain:
- **Snapshot fallback**: serve synced Pokémon from the local snapshot when PokeAPI is down (D-030).
- **Resync**: refresh a local record's snapshot from PokeAPI, keeping the custom attributes.
- **Localized names per language**: a `(language, name)` table, with `displayName` chosen by
  `Accept-Language` (D-027).
- **Per-user collections** ("my Pokémon", nicknames per user). This is a different product from the
  brief's shared, internal data (D-030).
- **Roles** (ADMIN-only remove), refresh tokens, httpOnly cookie session.
- An audit trail: who synced or last edited a record (`UserId` columns).
- A "local only" filter and a tag filter on the list.
- Alternate forms: `GET /pokemon` lists forms after #1025 (e.g. `deoxys-attack`), and
  `GET /pokemon/deoxys` is a 404. Paging over `/pokemon-species` would fix both. Until then, it's a
  known limitation in the README.

API and platform:
- Native Spring API versioning (D-013, superseded by D-029).
- Client-visible optimistic locking: `version` in the response, `If-Match` on PUT (D-011).
- `Cache-Control` headers on the merged reads.
- Rate limiting and a circuit breaker around PokeAPI.
- Demo data in its own Flyway location, off in tests and in non-demo environments.
- `micrometer-registry-prometheus` came with the scaffold without a decision: remove it or justify it.
- A GitHub Actions CI (`./gradlew check`, frontend lint/test/build).

Quality tooling:
- Three ArchUnit rules that need a custom `ArchCondition`: domain model types in generic return
  signatures of `interfaces`, `@Version` on every `@Entity`, `{Id}.generate()` called only from
  `interfaces`. They're enforced by review and ITs until then.
- E2E tests (Playwright) for the demo flow.

Frontend extras:
- A toast system, lazy-loaded routes, dark mode, i18n of the UI.
