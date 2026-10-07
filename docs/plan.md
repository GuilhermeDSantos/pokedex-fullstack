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

> **Slice 5 — Edit and remove our fields.** Next task: S5.3 (`PUT` and `DELETE /pokemon/{number}/local`). S5.1–S5.2 are done: the rules, the use cases, the adapter with optimistic locking; the `/local` routes take the Pokédex number (D-040). Slice 4 works end to end on Docker: sign in from the detail page, sync, and our fields appear merged into the page. Slice 3 works end to end on Docker. Slice 2 works end to end on Docker against the real PokeAPI. Phase 1 (foundation) is done.
> Phase 0 is done: the whole stack runs with `docker compose up --build` and `./gradlew check` is
> green. No blockers. The agent never commits or pushes before the developer has read the changes.

---

## Priorities

**The brief comes first.** Anything not needed to satisfy a row in `requirements.md` is in
"Parked" and stays there until every slice below is done.

If scope has to shrink, cut in this order (each step keeps every requirement covered):
1. Frontend tests trimmed to one happy path + one error path per page (the console guard stays).

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
- [x] S1.8 Setup (D-020, D-021, D-023, D-037): React Router 8 (`RouterProvider` from
      `react-router/dom`), TanStack Query, Vitest 5 + RTL + user-event + jsdom + MSW 2 + jest-dom.
      npm resolved MSW 2.15, not 3: Vitest's own mocker declares `msw ^2.4.9` as a peer, so 2.x keeps
      the tree valid. `src/test/setup.ts`: MSW fails unhandled requests, and the console guard fails
      any test that writes `console.error`/`console.warn` (proven with a throwaway test: error,
      warning and an unmocked request all fail). `typecheck` and `test` scripts. `AppShell` (banner
      link + `main`) and a not-found page, test-first through the real route table. Neutral
      `tokens.css` + `base.css` (D-034). Template content removed, neutral favicon (no 404).
      Checked in the browser: empty console, 360px layout. The Docker image still builds.
- [x] S1.9 `shared/api/httpClient`, test-first with MSW: base path `/api/v1` (`VITE_API_BASE_URL`),
      JSON bodies, `Authorization: Bearer` only when the call is given a token, `ErrorResponse` →
      typed `ApiError` (`status`, `code`, `message`, `fieldErrors`), a non-JSON error (e.g. nginx's
      502 page) → `UNEXPECTED_RESPONSE`, no response at all → `NETWORK_ERROR` (status 0). The client
      stays free of session rules: clearing the session on a 401 and dropping an expired token move
      to S1.10, where the session lives. `shared/ui`: `TextField` (label, controlled,
      `aria-invalid` + the error as accessible description), `Button` (`type="button"` by default,
      disabled while pending), `ErrorState` (`role="alert"` + Retry). `Heading` and `Stack` come
      with the S1.10 forms, the first code that uses them. The ESLint `no-restricted-imports` rule
      for `app → features → shared` comes with the first feature.
- [x] S1.10 `/login` and `/register` (controlled inputs + `requireFields`, D-022), test-first
      through the real route table (`renderApp`): sign in returns to `returnTo`; 401 → form error,
      no redirect; missing fields are asked for before any request; sign up signs the user in
      straight away; 409 → on the email field; a server rule (password policy) → form error; server
      `fieldErrors` → on their fields; each page links to the other keeping `returnTo`.
      `safeReturnTo` refuses other sites, including `//host` and the `/\host` bypass. Session:
      `AuthProvider` + `useAuth` + `authContext` (three files: fast refresh wants component files to
      export only components), restored from `sessionStorage` (D-024), an expired or malformed
      stored session is dropped, sign out clears it. Header shows the user + "Sign out", or "Sign in"
      / "Create account". `shared/ui`: `Heading`, `Stack`, `FormError`. ESLint
      `no-restricted-imports` enforces `app → features → shared` (proven with forbidden imports).
      Checked on the Docker stack in the browser: sign up (password policy error, then success),
      reload keeps the session, sign out, wrong password, sign in, duplicate email (upper-case,
      normalized by the backend) → email field. Clearing the session on a 401 from a protected call
      moves to S4.5: before the first protected write there is no call that can trigger it.
      The developer rejected the password message ("…and at most 72 bytes") on review: a BCrypt
      detail users can't act on. Split into `WeakPasswordException` (what to do) and
      `PasswordTooLongException` ("Password is too long"), and the sign-up form now shows the rule up
      front as a `TextField` hint (described after any error).
      **FE-5, accepted by the developer:** Chrome itself logs "Failed to load resource" for every
      4xx, so the expected 400/401/409 of these error paths show in the console. App code can't
      suppress it; the app writes nothing and the happy paths are clean (walkthrough FAQ).

## Slice 2 — Browse the list (US-01, US-01.N, TR-CACHE, FR-0, FE-2)

The list comes straight from PokeAPI here. Local data joins it in Slice 6.

Backend:
- [x] S2.1 Domain (TDD, plain JUnit): `PokedexNumber` (≥ 1, else `InvalidPokedexNumberException`,
      400; boundary proven by mutation), `Weight` (kg from PokeAPI's hectograms, one decimal so
      `6` equals `6.0`, negative rejected), `PokemonType` (trimmed, lower-cased), `Ability` (the
      brief's skills), `PokemonSummary` (number, name and weight required; sprite and category
      nullable; immutable lists), and the `PokemonRepository` port (`findAll` only) with
      `PokemonDataUnavailableException`. Data that comes from PokeAPI and breaks a rule is a
      mapping bug (`IllegalArgumentException`, 500), not a 400. `PokemonIdentifier` and
      `findByIdentifier` move to S3.1: only the detail route uses them.
- [x] S2.2 Fixtures recorded from pokeapi.co with `curl` under `src/test/resources/pokeapi/` (a list
      page, bulbasaur and pikachu with their species), trimmed of `moves`, `game_indices` and
      `sprites.versions` (unused, ~95% of the size), everything else as served. The field names and
      units in `domain-model.md` → PokeAPI mapping were checked against these real responses and
      match. `PokeApiTranslator` (pure, in `infrastructure/external/pokeapi`, JSON shapes as
      package-private records) → `PokemonSummary`, test-first: number and name, hectograms → kg,
      English genus as category (none → `null`), default front sprite (none → `null`), types and
      abilities in slot order (proven with reversed input, since the real arrays come sorted), the
      hidden ability marked. No id parsing from list URLs: `findAll` fetches each Pokémon, whose
      JSON has its `id`.
- [x] S2.3 `spring-boot-starter-restclient` + `-test` (D-038, verified on Maven Central for
      4.1.1). `PokeApiClient` on the auto-configured `RestClient.Builder` with `pokeapi.base-url`
      (`PokeApiProperties`); timeouts from Boot's `spring.http.clients.connect-timeout` (2s) /
      `read-timeout` (3s), the non-deprecated names in 4.1.1's metadata. `@RestClientTest` with
      recorded JSON: fetch a Pokémon, 404 → empty, 500 and I/O error → `PokemonRepositoryUnavailable`,
      species fetched from the URL PokeAPI gave (404 there → unavailable: PokeAPI contradicted
      itself), a list page. `PokeApiClientTimeoutIT` runs a real slow HTTP server (JDK
      `HttpServer`): the call gives up at ~3.3s with a 503-bound error, which proves the timeout
      property reaches our client. The developer asked whether PokeAPI should be a repository;
      kept as `PokemonRepository`, with the `*Repository` / `*Source` rule now explicit in
      `domain-model.md` and the walkthrough FAQ.
- [x] S2.4 `PokeApiPokemonRepository implements PokemonRepository`: `findAll` fetches each card's Pokémon
      and species concurrently on virtual threads and joins them in PokeAPI's order (D-018; proven
      with a latch: Bulbasaur's answer waits until Pikachu's request has started), capped by
      `pokeapi.max-concurrency` (10) through one shared `Semaphore` (peak of exactly 2 with a cap of
      2; mutation without `acquire` fails). A cap below 1 refuses to start (it would hang every
      list). A listed Pokémon PokeAPI can't return → unavailable; a worker's failure reaches the
      caller unwrapped (mutation proven). Caffeine cache (D-012): `CacheConfig` + `@Cacheable` on
      the client's three methods only, `maximumSize=2000,expireAfterWrite=6h`. Tests: each resource
      fetched once, a failure is not cached, and a repeated `findAll` through the port makes no
      new HTTP call (mutation without `@Cacheable` fails). The developer asked about merging client
      and source; kept apart because a merged class would self-call past the cache proxy (FAQ).
      `pokeapi-evolution-chains` comes with Slice 3.
- [x] S2.5 `BrowsePokemonInteractor` (TDD: the page of cards with its metadata; an invalid page
      never reaches PokeAPI), `PageOutput.totalPages()` (a partial last page counts). Wired in
      `UseCaseConfig`: `ApplicationContextIT` went red until it was, as designed. `PageResponse`,
      `PokemonSummaryResponse`, `PokemonRestMapper`, `PokemonController` `GET /api/v1/pokemon`
      (defaults `page=0`, `size=20`; public). `PokemonControllerIT`: 200 (strict shape, no token),
      defaults, 400 size out of range, 400 page not a number, 503 `DATA_UNAVAILABLE`. The 503 body
      has a fixed message: the exception's own can name internals, so it only goes to the log
      (the reference example echoed it; corrected). Measured on Docker against the real PokeAPI:
      a cold page of 20 (41 upstream calls) in 0.94s, the same page cached in 0.008s, another cold
      page in 0.53s. `displayName` and `synced` join the card with Slice 6 (the merge).

Frontend:
- [x] S2.6 `PokemonListPage` on `/`, test-first through the real route table: each card shows the
      brief's four fields (sprite, category, mass in kg, skills = abilities, the hidden one marked)
      plus name, number and type badges; skeletons while loading (`role="status"`); 503 →
      `ErrorState` whose Retry loads the page; an empty page → `EmptyState` linking to the first
      page; the page comes from the URL, **counted from 1** (`?page=3` asks the API for page 2) and
      anything else (`abc`, `0`, `-2`, `1.5`) means the first page; Previous/Next are links, absent
      at the edges. Cards: no sprite → "No image", no category → "Category unknown". Formatters in
      `shared/lib` (slug → name, `#025`, `6.0 kg`). `features/pokemon/{api,hooks,components,pages}`,
      query keys in `pokemonKeys`. `shared/ui`: `Card`, `Badge`, `Skeleton` (a variant, no inline
      style), `EmptyState`, `Pagination`. Default MSW handlers in `src/test/msw/handlers.ts`, shaped
      like the API. Found in the browser pass: after "Next page" the new page opened scrolled to its
      bottom; fixed with `<ScrollRestoration />` (top on navigation, position restored on Back), with
      a test (jsdom's `scrollTo` is stubbed in the setup: it only logs "not implemented"). Checked
      on Docker against the real PokeAPI: pages 2→3, Back, 360px in one column with no horizontal
      scroll, empty console.

## Slice 3 — View a Pokémon (US-02, FE-2)

Backend:
- [x] S3.1 Domain (TDD, plain JUnit): `PokemonIdentifier` (trimmed, lower-cased, `[a-z0-9-]{1,100}`,
      else `InvalidPokemonIdentifierException`, 400; `isNumber`/`asNumber` wait for the local
      lookup in Slice 4, since PokeAPI takes a name or a number as is), `Height` (m from
      decimetres, one decimal, never negative), `StatName` + `BaseStat` (1..255, ends proven by
      mutation), `EvolutionStage` (a tree; its own copy of the branches, none = a leaf),
      `PokemonProfile` (name, height, weight required; stats in the games' order, exactly one per
      stat), `PokemonDetail`. `PokemonNotFoundException` and the port's `findByIdentifier` /
      `getByIdentifier` move to S3.2, with the adapter that implements them, so no step leaves the
      port unimplemented.
- [x] S3.2 Fixtures: eevee and the evolution chains 10 (pichu → pikachu → raichu) and 67 (eevee →
      8 branches), checked against the real API: stats come as `special-attack` etc., a chain names
      each species only by URL (the Pokédex number is parsed from it), and old flavor texts carry
      `\f` and soft hyphens. `PokeApiTranslator.toDetail`: height in m, official artwork, stats in
      the games' order, the newest English description (highest version id, order not trusted)
      with breaks and form feeds read as single spaces and soft hyphens dropped, the whole lineage
      tree, null artwork or description accepted. Client `fetchEvolutionChain` (cached,
      `pokeapi-evolution-chains`). Port `findByIdentifier` + default `getByIdentifier` →
      `PokemonNotFoundException` (404). `PokeApiPokemonRepository.findByIdentifier` follows Pokémon →
      species → chain by the URLs PokeAPI gave; a repeated `getByIdentifier` (the default method's
      self-call) is served from the cache.
- [x] S3.3 `GetPokemonInteractor` (TDD; the identifier built by a pure `PokemonMapper`; a malformed
      one never reaches PokeAPI) and `GET /api/v1/pokemon/{identifier}`: `PokemonControllerIT` 200
      (strict shape), 404, 400, 503. Checked on Docker against the real PokeAPI: eevee's 8 branches,
      `missingno` → 404, `pika chu` → 400.

Frontend:
- [x] S3.4 `PokemonDetailPage` on `/pokemon/:identifier`, test-first: artwork (else the sprite,
      else "No image"), name, number, types, description, category/height/weight, abilities, the
      six base stats with the games' labels, and the evolution tree (every branch a link, the
      current one `aria-current`); loading skeleton, 404 → "Pokémon not found" with a link back,
      other errors → `ErrorState` with retry. The list card's name links to it. `TypeList` and
      `AbilityList` shared by card and detail. Found in the browser pass: TanStack Query retried
      the 404 too (with backoff, paused while the tab is hidden), so "not found" never showed;
      `retryPolicy` now never retries a 4xx (the 5xx count stays for U.4). Checked on Docker:
      list → Ivysaur, Eevee's 8 branches → Sylveon (scrolled to the top), `missingno` → not found
      within a second.
- [x] S3.5 Review follow-ups, decided by the developer: the domain never names PokeAPI, not even in
      comments; every data port is a **repository** (`PokemonSource` → `PokemonRepository`, its
      read models moved to `domain/model`, adapter `PokeApiPokemonRepository`); and data that can't
      be reached is handled the same wherever it lives: abstract `DataUnavailableException` with one
      subclass per repository (+ `TransactionUnavailableException`), one 503 `DATA_UNAVAILABLE`
      (contract updated first). `DatabaseUnavailableIT` stops PostgreSQL for real: sign-in and
      registration answered 500 after 30.9 s; now 503 within Hikari's 3 s. `PokemonSummary` keeps its
      name, the pair of `PokemonDetail`; the merged "one Pokémon" is what the API returns (D-030).

## Slice 4 — Sync a Pokémon into the local database (US-03, TR-DAL, TR-API-1)

Backend:
- [x] S4.1 Domain (TDD): `LocalPokemonId`, `Tag`, `CustomAttributes`, the `LocalPokemon`
      aggregate (`create`, `displayName(canonicalName)`) + `PokemonAlreadySyncedException`,
      `LocalPokemonNotFoundException`, `PokemonIdentifier.isNumber()`. No `PokemonSnapshot` and no
      name: the record is the Pokédex number + our fields (D-039, the developer's call in review).
      `Tag`'s rules wait for the first write that takes tags (S5.1).
- [x] S4.2 Migration `V2__create_local_pokemons.sql` (`local_pokemons` + `local_pokemon_tags`, index
      on `tag`), then `LocalPokemonEntity`, `LocalPokemonJpaRepository`, `LocalPokemonEntityMapper`,
      `JpaLocalPokemonRepository` + IT (whole-aggregate round trip with tags, unique number → 409).
      `DatabaseFailures` is the one rule for "unreachable": the full suite showed a connection
      that dies between two requests answered 500; `DatabaseConnectionLostIT` now proves 503.
- [x] S4.3 Interactors (TDD): `SyncPokemon` (no transaction during the PokeAPI call),
      `GetLocalPokemon`. `GetPokemon` now merges the local record into the detail (`local`, `null`
      when not synced).
- [x] S4.4 `POST` and `GET /pokemon/{identifier}/local` + IT (201 + `Location`, 401 without a token,
      404 not in PokeAPI / not synced, 409 already synced, 503), and the `local` field in the detail
      response.

Frontend:
- [x] S4.5 Our fields on the detail page: **Sync to local database** when `local` is null ("Log
      in to sync" with `returnTo` when signed out). A 409 (someone just synced it) refetches and
      shows the local data with an inline note. A 401 on the sync (expired token, or the account is
      gone, D-033) clears the session and goes to `/login` with `returnTo` (moved here from S1.10).
      Tests: signed out → "Log in to sync", sync → our fields appear, 409 → refetch + note,
      401 → signed out and sent to sign in.
      After review, no "Local data" block: the name stays the title with the localized name under
      it, the region joins the facts and the tags close the page as badges (`frontend.md`).

## Slice 5 — Edit and remove the local data (US-04, US-04.a–c, TR-API-1, FE-3)

Backend:
- [x] S5.1 Domain (TDD): `LocalPokemon.updateCustomAttributes` and the custom-attribute validation
      (tag format, text and tag-count limits). `LocalPokemonModifiedConcurrentlyException` moved to
      S5.2, next to the adapter that throws it.
- [x] S5.2 Interactors (TDD): `UpdateLocalPokemon`, `RemoveLocalPokemon`. First the developer's
      D-040: every `/local` route takes the Pokédex number (`PokedexNumber.parse`, a name is a 400),
      so get-local, update and remove never call PokeAPI; sync checks existence by number. Adapter:
      an edit updates the managed entity (`copyInto`, D-011), so repeated edits work and a
      concurrent one is a 409 (`LocalPokemonModifiedConcurrentlyException`, two-transaction IT);
      delete flushes, so failures are translated. Frontend sync sends the number and refreshes every
      cached detail (a page opened by number didn't refresh after a sync; fixed and tested).
- [ ] S5.3 `PUT` and `DELETE /pokemon/{identifier}/local` + IT: 200/204, 400 invalid **and**
      malformed body, 401, 404 not synced, 409 concurrent edit.

Frontend:
- [ ] S5.4 New global component `ConfirmDialog`. Inline edit form for the localized name, region
      and tags (controlled inputs, server field errors mapped), and **Remove** with a confirmation
      dialog. After either, the detail refetches. Tests: save sends the PUT and shows the new
      values, a 400 maps to field errors, cancelling the dialog removes nothing, confirming does.

## Slice 6 — Edits show up everywhere (US-01, US-02, US-03.a)

The list and the detail reflect the local data. On the detail the name stays the title, with the
localized name under it (the developer's call in the S4 review); the list card should follow the
same rule, which S6 settles.

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
      plain demo password appears only in the README. The Pokédex numbers come from real
      PokeAPI responses recorded with `curl`, never typed from memory. **Pikachu is not in the
      seed**: it's synced live in the demo (a 201, not a 409) and it's the test fixture.
- [ ] D.2 `ApplicationContextIT`: context loads, every `*UseCase` bean resolves, seed present.
- [ ] D.3 Clean `docker compose up --build` from scratch (`down -v`), with the seed, and the whole
      demo flow clicked through.
- [x] D.4 OpenAPI UI via springdoc (D-016), brought forward to the end of Slice 1: springdoc 3.1.1
      (built on Boot 4.1.0), `/v3/api-docs` and `/swagger-ui.html` public, a `bearer-jwt` scheme
      on protected operations (`@SecurityRequirement` on the controller; the scheme is declared in
      `interfaces/rest/OpenApiDocumentation`, so `interfaces` never imports `infrastructure`).
      `OpenApiIT`: contract and UI without a token, title, scheme on `/auth/me` and not on login.
      Checked in the browser on Docker: the UI loads, Authorize works, empty console. Every new
      protected endpoint carries `@SecurityRequirement(name = OpenApiDocumentation.BEARER_JWT)`.
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
- [ ] U.4 The one behaviour change of the pass, test-first: TanStack Query retries a failed query 3
      times by default, so with PokeAPI down the list shows skeletons for ~7s before the error.
      Retry once, on 5xx and network errors only (scheduled here by the developer during S2.6).

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
- **Offline fallback**: serve synced Pokémon when PokeAPI is down. It needs a copy of PokeAPI's
  data, which D-039 dropped on purpose (D-030).
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
