# Execution Plan

The living backlog. **The agent reads this at the start of every session and updates it at the end
of every task.** It ticks the box, adds evidence to [`requirements.md`](requirements.md), and moves
"Current focus".

The work is organized in **vertical slices** (D-033). After the foundation, each slice delivers one
user action working end to end, backend **and** frontend, and runnable with
`docker compose up --build`. Inside a slice, the order is always TDD inward-out: domain →
interactor → adapters → controller → screen. The product being built is described in
[`domain-model.md` → Product vision](domain-model.md#product-vision).

---

## Current focus

> **Slice 1 — Sign up, sign in, sign out.** Next task: S1.8 (frontend setup). Phase 1 (foundation) is done.
> Phase 0 is done: the whole stack runs with `docker compose up --build` and `./gradlew check` is
> green. No blockers. The agent never commits or pushes before the developer has read the changes.

---

## Priorities

**The brief comes first.** Anything not needed to satisfy a row in `requirements.md` is in
"Parked" and stays there until every slice below is done.

If scope has to shrink, cut in this order (each step keeps every requirement covered):
1. Springdoc (D.4). The README `curl` examples cover it.
2. Frontend tests trimmed to one happy path + one error path per page (the console guard stays).

Never cut: TDD on domain and interactors, the ArchUnit test, the error-status ITs, the console
guard, Docker, the README, the GenAI case study.

**A slice is done** when its user action works in the browser on the Docker stack, `./gradlew
check` and the frontend checks (`lint`, `typecheck`, `test`, `build`) are green, the console is
empty, and `requirements.md` has the evidence.

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

Everything every slice needs.

- [x] 1.1 `LayeredArchitectureTest`: 17 ArchUnit rules from
      [`examples/tests.md`](examples/tests.md#architecture-test), green on the empty skeleton.
      Proven with a throwaway domain class (Spring annotation + `Instant.now()` + public
      constructor): exactly those three rules failed. The temporary `failOn…` lines in
      `build.gradle` are removed. **Temporary:** `archunit.properties` allows empty rules until
      every layer has classes (removed in S1.7).
- [x] 1.2 Domain kernel, in 5 TDD cycles: `DomainException`, `ValidationException`,
      `InvalidPageRequestException`; `PageRequest` (page ≥ 0, size 1..50, `offset()`) and `Page`
      (immutable copy, `map`). The other categories (`NotFound`, `Conflict`, `Unauthenticated`) are
      created in 1.4, with the handler and the tests that map them, so none is born untested.
      Boundary sizes 1 and 50 are accepted (proven by mutating `>` into `>=`).
- [x] 1.3 `UnitOfWork` port (the `Runnable` overload runs inside the same boundary),
      `SpringUnitOfWork` on a `TransactionTemplate` (`SpringUnitOfWorkIT` on real Postgres: commits
      and returns the result; rolls back and rethrows, proven against an adapter without a
      transaction), `ClockConfig` (UTC system clock). `UseCaseConfig` is created in S1.4 with the
      first interactor it wires, not as an empty class.
- [x] 1.4 `NotFoundException`, `ConflictException`, `UnauthenticatedException`; `ErrorResponse`;
      `GlobalExceptionHandler`, in 11 TDD cycles + a refactor. `GlobalExceptionHandlerIT` uses
      exceptions that exist only in the test, proving mapping by category: 404/409/400/401/422,
      malformed JSON and invalid body (with field errors) → 400, missing or mistyped parameter → 400,
      unknown path → 404, unexpected → 500 without leaking the cause, Spring's 405/415 kept. Two
      defects found by the tests: validation messages followed the browser language (fixed with a
      fixed English locale), and the catch-all turned 405/415 into 500. `PageResponse` and the 503
      mapping move to S2.5, with the first endpoint that needs them.

---

## Slice 1 — Sign up, sign in, sign out (TR-DB-1, TR-AUTH-1..3, FE-1, FE-4)

First, so every protected endpoint of the later slices is born behind the real security config,
with its 401 tests.

Backend:
- [x] S1.1 Domain, in TDD cycles + refactors: `Email` (trimmed, lower-cased, format, ≤ 254),
      `FullName` (the user's name, one free-text field, trimmed, 2..100; renamed from
      `DisplayName`), `RawPassword` (8 chars to 72 **bytes**, a letter and a digit, redacted
      `toString`), `PasswordHash`, `UserId`, the `UserAccount` aggregate, and their validation
      exceptions, whose messages are built from the VO's limits and read consistently (also
      `InvalidPageRequestException`, now with named factories). The other exceptions
      (`EmailAlreadyRegistered`, `InvalidCredentials`, `UnknownAccount`) come in S1.4 with the
      interactors that throw them.
- [x] S1.2 `V1__create_user_accounts.sql` (named PK and `uk_user_accounts_email`), the
      `UserAccountRepository` port (`save`, `findById`, `findByEmail`), `UserAccountEntity`, the
      Spring Data interface, the entity mapper and `JpaUserAccountRepository`, in 3 TDD cycles + a
      test refactor. `JpaUserAccountRepositoryIT` on real Postgres and the real migration: whole
      round trip, find by email, duplicate email → `EmailAlreadyRegisteredException` (409; only that
      constraint is translated, anything else stays loud). The adapter is wired by Spring in the
      test (`@Import`), not built by hand.
- [x] S1.3 Ports `PasswordHasher`, `TokenIssuer` (+ `AccessToken`), then the adapters
      `BCryptPasswordHasher` and `JwtTokenIssuer`, plus `JwtProperties` (`security.jwt.*`: secret,
      TTL, issuer; refuses to start with a secret under 32 bytes) and a `JwtConfig` creating the
      HS256 `JwtEncoder`/`JwtDecoder`. Tests: hash round trip, token round trip, expired token and
      token signed with another secret rejected. `JWT_SECRET` in `docker-compose.yml` and
      `.env.example` with dev-only defaults. 32 bytes verified against Nimbus 10.9.1 (31 rejected).
      A minimal `SecurityConfig` came forward from S1.5: the `JwtDecoder` bean switches on the
      resource server's default chain, which closed `/actuator/health` (caught by
      `ApplicationHealthIT`).
- [x] S1.4 Interactors (TDD), with `InvalidCredentialsException` and `UnknownAccountException`
      (`EmailAlreadyRegisteredException` already came with S1.2): `RegisterUser` (hash outside the
      transaction, `findByEmail` → 409, invalid input never reaches a port), `AuthenticateUser`
      (same 401 and message for unknown email and wrong password, proven by mutation),
      `GetCurrentUser` (account gone → 401 `UnknownAccountException`, D-033). `UserAccountMapper`
      builds `Registration`/`Credentials`/`UserId`. `UseCaseConfig` wires them, and
      `ApplicationContextIT` discovers every `*UseCase` interface and requires exactly one bean.
- [x] S1.5 `SecurityConfig` completed with the D-030 policy, **closed by default** (D-035): the
      public routes are listed (`GET /api/v1/pokemon/**`, register, login, health) and everything
      else needs a token. `ErrorResponseAuthenticationEntryPoint` (`interfaces/rest/security/`)
      writes the 401 `ErrorResponse`, registered on the resource server so it covers a missing and
      an invalid token (proven by mutation). `SecurityConfigIT` (probe controller on the real
      paths): missing/invalid token → 401 `ErrorResponse`, public reads, register and login,
      protected writes + `/auth/me` + an undeclared route → 401, authenticated → 200. No 403
      writer: without roles nothing can answer 403, so it waits for the first role.
- [x] S1.6 `AuthController` + `AuthRestMapper` (requests `RegisterUserRequest`,
      `AuthenticateUserRequest` with required-ness only; responses `UserResponse`,
      `AccessTokenResponse` with `tokenType: "Bearer"`). The controller is the edge: `UserId.generate()`
      and `Instant.now(clock)`; `/auth/me` reads the user id from the token's subject.
      `AuthControllerIT`: register 201/400 (missing fields, malformed, rejected by the domain)/409,
      login 200/400 (missing fields, malformed)/401, `/auth/me` 200/401 (no token, vanished account).
      Found while testing: an expired or invalid token sent to a **public** route answered 401.
- [x] S1.6b Public routes ignore the bearer token (D-036): one `PUBLIC_ROUTES` matcher drives
      `permitAll` and a `BearerTokenResolver`. `SecurityConfigIT`: an invalid token on every public
      route → 200, and a **real** issued token on a protected route → 200 (`jwt()` skips the
      resolver; a resolver that ignores every token fails only this test, proven by mutation).
- [x] S1.7 `AuthFlowIT` (`@SpringBootTest` + Testcontainers): register → login → `/auth/me` with the
      **real** issued token, plus a garbage token → 401 `ErrorResponse`. A token whose subject is the
      email instead of the id fails only this test (mutation), not `AuthControllerIT`.
      `archunit.properties` deleted, so a rule that matches nothing fails again. That exposed one
      rule whose `that()` selected the violation itself (methods annotated `@Transactional`), which
      is empty in correct code; rewritten to select every method outside the transaction adapter
      (same meaning, proven by mutation).

Frontend (every task below is test-first with RTL + MSW, one behaviour at a time; the MSW
handlers mirror the API contract):
- [ ] S1.8 Setup (D-020, D-021, D-023): router, query client, Vitest + RTL + MSW, the console-guard
      test setup, a `typecheck` script, the `AppShell` layout with a header. `app/styles/tokens.css`
      with **neutral** values (system font, greys, spacing scale) and a minimal base stylesheet
      (D-034: UX first, UI later). The Vite template's demo content and assets are removed.
- [ ] S1.9 `shared/api/httpClient` (base path `/api/v1`, auth header only on protected calls,
      `ApiError` from `ErrorResponse`, 401 handling) + tests; the first global components in
      `shared/ui`: `Button`, `TextField`, `Heading`, `Stack`, `ErrorState`.
- [ ] S1.10 Sign up and sign in pages (controlled inputs, D-022; server field errors mapped),
      `AuthContext` with session restore from `sessionStorage` (D-024), sign out, and the header
      showing the signed-in user. A successful sign up signs the user in straight away and returns
      to `returnTo` (or the list). Tests: field errors on 400, "email already registered" on 409,
      "invalid email or password" on 401 (a form error, not a redirect), session survives a reload,
      sign out clears it.

## Slice 2 — Browse the list (US-01, US-01.N, TR-CACHE, FR-0, FE-2)

The list comes straight from PokeAPI here. Local data joins it in Slice 6.

Backend:
- [ ] S2.1 Domain VOs (TDD): `PokedexNumber`, `PokemonIdentifier`, `Weight`, `PokemonType`,
      `Ability`, plus `PokemonSummary` and the `PokemonSource` port with
      `PokemonSourceUnavailableException`.
- [ ] S2.2 Record PokeAPI fixtures with `curl` under `src/test/resources/pokeapi/` (a list page,
      bulbasaur, pikachu with their species). `PokeApiTranslator` for summaries + tests (units,
      English genus as category, slot order, null sprite).
- [ ] S2.3 Add `spring-boot-starter-restclient` and `spring-boot-starter-restclient-test` (Boot 4
      split them out, see backend.md). `PokeApiClient` (`RestClient`, timeouts, 404 → empty,
      failures → `PokemonSourceUnavailableException`) + `@RestClientTest`.
- [ ] S2.4 `PokeApiPokemonSource.findAll` (concurrent fan-out on virtual threads, D-018) and the
      Caffeine cache on `PokeApiClient` only (D-012), with a test that a repeated call makes no HTTP
      request.
- [ ] S2.5 `BrowsePokemonInteractor` (TDD), then `PageResponse`, the 503 mapping of
      `PokemonSourceUnavailableException` in `GlobalExceptionHandler`, and `PokemonController`
      `GET /pokemon` + `PokemonControllerIT` (200, 400 page/size, 503).

Frontend:
- [ ] S2.6 List page (the home page): every card shows the brief's four fields (sprite, category,
      mass in kg, skills = abilities) plus name and number. Responsive grid, pagination in the URL,
      skeletons, error and empty states. New global components: `Card`, `Skeleton`, `Pagination`,
      `EmptyState`, `Badge`. Tests: the four fields render, loading → success, 503 → `ErrorState`
      with retry, empty page → `EmptyState`, the page number follows the URL.

## Slice 3 — View a Pokémon (US-02, FE-2)

Backend:
- [ ] S3.1 Domain (TDD): `Height`, `BaseStat`/`StatName`, `PokemonProfile`, `EvolutionStage`,
      `PokemonDetail`, `PokemonNotFoundException`.
- [ ] S3.2 Fixtures for eevee (branching chain) and an evolution chain. `PokeApiTranslator` for
      details + tests (flavor-text normalization, highest English version, branching evolution
      tree, null artwork). `PokeApiPokemonSource.findByIdentifier`, cached through the client, with
      the test that goes through the port's `getByIdentifier` (the self-invocation trap).
- [ ] S3.3 `GetPokemonInteractor` (TDD), then `GET /pokemon/{identifier}` + IT (200, 400, 404, 503).

Frontend:
- [ ] S3.4 Detail page: artwork, name and number, stats, description, the evolution tree
      (branching), loading/error/not-found states. A card on the list opens it. Tests: every US-02
      field renders, a branching chain renders every branch, 404 → not-found state.

## Slice 4 — Sync a Pokémon into the local database (US-03, TR-DAL, TR-API-1)

Backend:
- [ ] S4.1 Domain (TDD): `LocalPokemonId`, `PokemonSnapshot` (+ `PokemonProfile.toSnapshot()`),
      `Tag`, `CustomAttributes`, the `LocalPokemon` aggregate (`create`) +
      `PokemonAlreadySyncedException`, `LocalPokemonNotFoundException`.
- [ ] S4.2 Migration `V2__create_local_pokemons.sql` (`local_pokemons` + `local_pokemon_tags`), then
      `LocalPokemonEntity`, `LocalPokemonJpaRepository`, `LocalPokemonEntityMapper`,
      `JpaLocalPokemonRepository` + IT (whole-aggregate round trip, find by name and by number,
      unique constraint → 409).
- [ ] S4.3 Interactors (TDD): `SyncPokemon` (no transaction during the PokeAPI call),
      `GetLocalPokemon`. `GetPokemon` now merges the local record into the detail (`local`, `null`
      when not synced).
- [ ] S4.4 `POST` and `GET /pokemon/{identifier}/local` + IT (201 + `Location`, 401 without a token,
      404 not in PokeAPI / not synced, 409 already synced, 503), and the `local` field in the detail
      response.

Frontend:
- [ ] S4.5 Local section of the detail page: **Sync to local database** when `local` is null ("Log
      in to sync" with `returnTo` when signed out). A 409 (someone just synced it) refetches and
      shows the local data with an inline note. Tests: signed out → "Log in to sync", sync → local
      section appears, 409 → refetch + note.

## Slice 5 — Edit and remove the local data (US-04, US-04.a–c, TR-API-1, FE-3)

Backend:
- [ ] S5.1 Domain (TDD): `LocalPokemon.updateCustomAttributes`, `LocalPokemonModifiedConcurrentlyException`
      and the custom-attribute validation (tags, sizes).
- [ ] S5.2 Interactors (TDD): `UpdateLocalPokemon`, `RemoveLocalPokemon`. Optimistic locking in the
      repository adapter (`copyInto` the managed entity, D-011) + IT for the concurrent edit → 409.
- [ ] S5.3 `PUT` and `DELETE /pokemon/{identifier}/local` + IT: 200/204, 400 invalid **and**
      malformed body, 401, 404 not synced, 409 concurrent edit.

Frontend:
- [ ] S5.4 New global component `ConfirmDialog`. Inline edit form for the localized name, region
      and tags (controlled inputs, server field errors mapped), and **Remove** with a confirmation
      dialog. After either, the detail refetches. Tests: save sends the PUT and shows the new
      values, a 400 maps to field errors, cancelling the dialog removes nothing, confirming does.

## Slice 6 — Edits show up everywhere (US-01, US-02, US-03.a)

The list and the detail reflect the local data: the edited name replaces the original one, and the
original stays visible underneath.

Backend:
- [ ] S6.1 Domain (TDD): `LocalPokemon.displayName()` (localized name when set, otherwise the
      original).
- [ ] S6.2 The list merge: `findAllByPokedexNumbers` (one query per page) + IT, and
      `BrowsePokemon` adds `displayName` and `synced` to each item. `GetPokemon` adds `displayName`.
      Controller ITs for the new fields.

Frontend:
- [ ] S6.3 The cards and the detail title show `displayName`, with the original name underneath
      when they differ, plus a "synced" badge on the list. Sync, edit and remove invalidate both the
      detail and the list queries, so going back to the list shows the change at once. Tests: the
      display name and the original name render, the badge appears only for synced Pokémon, an
      edit is visible on the list without a manual reload.

---

## Delivery (DL-1..3, TR-OPT, FE-5)

- [ ] D.1 `V3__seed_demo_data.sql`: demo user + about 10 synced Pokémon with custom attributes
      and tags, so the list starts with merged data. The user's BCrypt hash is generated with the
      project's own `BCryptPasswordHasher` (a one-off run), never with an online generator. The
      plain demo password appears only in the README. The snapshot values come from real
      PokeAPI responses recorded with `curl`, never typed from memory. **Pikachu is not in the
      seed**: it's synced live in the demo (a 201, not a 409) and it's the test fixture.
- [ ] D.2 `ApplicationContextIT`: context loads, every `*UseCase` bean resolves, seed present.
- [ ] D.3 Clean `docker compose up --build` from scratch (`down -v`), with the seed, and the whole
      demo flow clicked through.
- [ ] D.4 OpenAPI UI via springdoc, **timeboxed to 30 minutes** (D-016). If it doesn't work on the
      first try, remove the dependency and move on.
- [ ] D.5 `README.md`: expand the current run instructions with the product overview, an
      architecture diagram, demo credentials, the API table, design decisions summary (linking
      `docs/decisions.md`), known limitations and next steps, and how AI was used.
- [ ] D.6 Frontend quality pass: zero console warnings on every page (manual, DevTools open),
      keyboard-only run, a manual check at ~360 px and desktop width.

## UI pass (FE-2)

After the slices and the delivery checks: the functional MVP becomes a polished one (D-034). Only
styles change; behaviour and tests stay as they are.

- [ ] U.1 Visual direction: colour palette (incl. Pokémon type colours), typeface, type scale,
      spacing and radius. Applied by changing the values in `tokens.css`.
- [ ] U.2 Component polish in `shared/ui` (states: hover, focus, disabled, loading), and the card
      and detail layouts.
- [ ] U.3 Visual check at ~360 px, tablet and desktop width, keyboard focus still visible, AA
      contrast still met, console still empty.

## GenAI case study (AI-1..5)

Independent of the Pokémon code: do it once Slice 5 is done, before the polish.

- [ ] G.1 Write the prompt in [`genai-case-study.md`](genai-case-study.md), run it, and save the raw output.
- [ ] G.2 Review the output against a checklist, then record the defects found and the fixes, with
      before/after snippets.
- [ ] G.3 Write up edge cases, auth and validation handling, plus lessons learned.

## Walkthrough (EV-1..6)

- [ ] W.1 Fill in the [`walkthrough.md`](walkthrough.md) agenda and demo script, and have the
      evidence for every row of `requirements.md` ready.
- [ ] W.2 Check the answers in the design FAQ against the final code, with a full dry run from
      `docker compose up`.
- [ ] W.3 Final check: every row in `requirements.md` is ✅ with evidence, the repo is public, the
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
- Null-safety with JSpecify: `@NullMarked` in each `package-info.java`, the dependency declared,
  and NullAway in the build. Spring 7 already marks its own packages, which is why the IDE suggests
  `@NonNull` on overrides. Half-adopting it in one class was reverted.
- `WWW-Authenticate: Bearer` on the security 401 (RFC 6750), without the error details.

Frontend extras:
- A toast system, lazy-loaded routes, dark mode, i18n of the UI.
