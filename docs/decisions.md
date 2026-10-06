# Decision Log

Lightweight ADRs. Every non-obvious technical choice gets an entry **before** the code that depends
on it. Code review will ask "why X and not Y?", and the answer should already be written here.

Status: **Accepted** (in force) · **Proposed** (waiting for the human's explicit approval — usually
a new dependency; don't implement until approved) · **Rejected** (considered and turned down; kept
so the question isn't reopened) · **Superseded by D-xxx**.

**2026-10-06: every open decision was resolved**, and the scope was kept to the brief (extras are
parked in `plan.md`). Nothing is Proposed right now.

Template for a new entry:

```
## D-0NN — Title
**Status:** Proposed | Accepted · **Date:** YYYY-MM-DD · **Requirements:** IDs
**Context:** the problem, in two or three sentences.
**Decision:** what we do.
**Alternatives considered:** what else, and why not.
**Consequences:** what this costs or commits us to.
```

---

## D-001 — Strict Clean Architecture: `domain` and `application` are framework-free
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** OV-2, TR-BL-2, EV-1
**Context:** The brief requires Clean Architecture explicitly ("independence of components", and a
business layer independent of the API and data access). A common Spring shortcut puts `@Service` and
`@Transactional` on use cases, which makes them depend on the framework.
**Decision:** Neither `domain` nor `application` imports anything outside `java.*` and inner layers.
No `@Service`, `@Component`, `@Transactional`, `@Value`, Jakarta Validation, Jackson, slf4j or
Lombok. ArchUnit **allowlists** enforce this for both layers. Transactions go through the
`UnitOfWork` port, and wiring happens in a composition root (D-003).
**Alternatives considered:** `@Service` on use cases. It's convenient, but it makes use cases
depend on Spring, which contradicts the Dependency Rule, and a reviewer can point to it.
**Consequences:** One `@Bean` per use case in `UseCaseConfig`. Interactors can't log, so failures
are logged by adapters. In exchange, `domain` + `application` could compile as a plain Java module,
and that's demonstrable.

## D-002 — Interactors return a response model (Output DTO); no presenter interfaces
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** OV-2, EV-1
**Context:** In Uncle Bob's canonical diagram, the interactor pushes a response model into an
*output boundary* that a presenter implements.
**Decision:** `execute(...)` **returns** a framework-free Output DTO `record`. The controller (via
its REST mapper) acts as the presenter.
**Alternatives considered:** An output boundary/presenter per use case. It's the literal reading,
but for a synchronous JSON API it adds one interface and one class per use case and changes
nothing about dependency direction. The Output DTO already points inward-to-outward, and the
interactor still knows nothing about HTTP or JSON.
**Consequences:** It's a simpler, widely accepted variant, and an explicit, defensible talking
point in the review.

## D-003 — Input ports are interfaces; `UseCaseConfig` is the composition root
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** OV-2, EV-1
**Decision:** `{Verb}{Name}UseCase` is an interface (input port), `{Verb}{Name}Interactor`
implements it, and controllers depend only on the interface. Interactors, application mappers and
application services are created in `infrastructure/config/UseCaseConfig` as `@Bean`s whose return
type is the port.
**Alternatives considered:** Controllers depending on concrete use case classes. It's simpler, but
it's a weaker boundary, and in the strict reading of Clean Architecture the controller should know
only the input boundary.
**Consequences:** ArchUnit rules make sure interfaces never reference `*Interactor`, and that each
interactor implements a `*UseCase`. A context-load IT catches a missing bean.

## D-004 — PokeAPI is a domain port (`domain/catalog/PokemonCatalog`)
**Status:** Accepted, renamed by D-032 (`domain/source/PokemonSource`) · **Date:** 2026-10-05 · **Requirements:** FR-0, US-01, US-02, US-03
**Decision:** The catalog is part of the domain's vocabulary ("browse/look up the authoritative
Pokédex"), and sync builds the aggregate from it, so the port and its read types live in
`domain/catalog`. `CatalogUnavailableException extends RuntimeException` (not `DomainException`) is
part of the port contract, so `interfaces` can map it to 503 without knowing the adapter.
**Alternatives considered:** A port in `application/port` returning application DTOs. That's
workable, but sync would then have to translate DTOs back into domain VOs, which duplicates
validation.
**Consequences:** The adapter translates PokeAPI JSON → domain VOs, and catalog records reuse
`PokemonProfile`.

## D-005 — PostgreSQL + Flyway; seed data as a migration
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** OV-4, TR-DB-1, DL-2
**Decision:** We use PostgreSQL (the driver was already in the scaffold) with
`ddl-auto: validate`, and Flyway owns the schema. Demo data (one user with a BCrypt hash, ~10
Pokémon with real data, custom attributes and tags) is `V3__seed_demo_data.sql` with fixed UUIDs.
That makes it deterministic and offline-capable.
**Alternatives considered:** H2, which differs from prod and makes the tests less trustworthy. A
startup sync from PokeAPI, which makes the demo depend on network and timing.

## D-006 — `Pokemon` aggregate = canonical `PokemonProfile` + our `CustomAttributes`
**Status:** Superseded by D-031 (scalar snapshot) and D-032 (`LocalPokemon`); the "ours vs theirs" split stands · **Date:** 2026-10-05 · **Requirements:** US-03, US-03.a, US-04
**Decision:** One aggregate per Pokémon. The catalog-owned data is a `PokemonProfile` VO, replaced
wholesale on resync. The proprietary data is a `CustomAttributes` VO (`localizedName`, `region`,
`tags`), replaced wholesale by PUT. Child values are stored with `@ElementCollection`, and the
Pokédex number is unique.
**Alternatives considered:** Editing canonical fields directly. That blurs the line between "ours"
and "theirs", and a resync would clobber the edits. Storing tags as a separate aggregate isn't
needed, because no invariant spans Pokémon.
**Consequences:** US-04 edits only proprietary fields. Canonical data comes only from the catalog,
at sync time. Refreshing it later (resync) is parked as a next step (`plan.md`). Say this
explicitly in the demo.

## D-007 — Exception categories, including `ValidationException` (400) and `UnauthenticatedException` (401)
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** US-04.a/b, TR-ERR
**Decision:** The categories are `NotFound`→404, `Conflict`→409, `Validation`→400,
`Unauthenticated`→401, and the `DomainException` catch-all→422. The handler is keyed only on
categories. Syntactic problems (Bean Validation, malformed JSON, bad params) →
400 with the same `ErrorResponse`.
**Alternatives considered:** Mapping every domain rule violation to 422. That's technically
defensible, but the brief explicitly asks for **400** on bad payloads, and an invalid tag in a PUT
body *is* a bad payload.

## D-008 — Stateless JWT with Spring Security's own JOSE support; BCrypt
**Status:** Accepted · **Date:** 2026-10-06 · adds `spring-boot-starter-oauth2-resource-server` · **Requirements:** TR-AUTH-1..3
**Decision:** `POST /auth/login` returns an HS256 JWT (1 h TTL) signed with a configured secret.
The API validates it as an OAuth2 resource server, and passwords are hashed with BCrypt. Both sit
behind `TokenIssuer`/`PasswordHasher` ports.
**Alternatives considered:** Server sessions with cookies need CSRF handling and are stateful. A
third-party JWT library (jjwt) is unnecessary, since Spring Security ships Nimbus. Keycloak is
overkill for the scope.
**Consequences:** There's no refresh token (documented limitation and possible improvement). The
secret comes from an env var in Docker.

## D-009 — Route policy: catalog public, local Pokédex protected
**Status:** Superseded by D-030 (every read public, only writes protected) · **Date:** 2026-10-05 · **Requirements:** TR-AUTH-3
**Decision:** Public: catalog browse/detail, register, login, health. Protected: everything under
`/pokemon` (list, get, sync, update, delete) and `/auth/me`. No roles yet: any authenticated user
can manage the shared local Pokédex.
**Alternatives considered:** Public local reads with protected writes is also valid. Requiring
login for the whole local Pokédex makes the public-vs-protected split obvious in the demo. Roles
(ADMIN for delete) are a listed possible improvement.

## D-010 — "Category" = species genus; "skills" = abilities
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** US-01
**Context:** The brief's US-01 asks for "category" and "skills", and PokeAPI has no fields with those
names.
**Decision:** **Category** is the English `genus` from `/pokemon-species` ("Seed Pokémon"), which is
what the official Pokédex calls "category". **Skills** are the Pokémon's **abilities**. Types are
shown too, as badges.
**Alternatives considered:** Category as type. But type is its own concept, and the Pokédex uses
"category" for genus. Skills as moves: there are up to ~100 per Pokémon, which is unreadable on a
card, so moves could be a detail-page extra.
**Consequences:** Each list entry needs a species call too. That's handled with concurrency and a
cache (D-012, D-018). State the interpretation in the walkthrough.

## D-011 — Optimistic locking: `@Version` checked against the managed entity
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** US-04.c
**Decision:** Every entity has `@Version`. The repository adapter updates the managed entity loaded
in the same `UnitOfWork`, and `OptimisticLockingFailureException` becomes
`PokemonModifiedConcurrentlyException` (409).
**Out of scope (parked 2026-10-06):** Exposing `version` in responses and requiring it (`If-Match`)
on PUT, so stale clients get a 409. Mention it as a next step, don't build it.

## D-012 — Cache: Spring Cache abstraction + Caffeine, on the PokeAPI adapter only
**Status:** Accepted · **Date:** 2026-10-06 · adds `spring-boot-starter-cache` + `com.github.ben-manes.caffeine:caffeine` · **Requirements:** US-01.N, TR-CACHE
**Decision:** `@Cacheable` goes only on `PokeApiClient`, a dedicated bean with one method per
PokeAPI resource (`pokeapi-pages`, `pokeapi-pokemon`, `pokeapi-species`,
`pokeapi-evolution-chains`), with a TTL and max size from `application.yaml`. Exceptions aren't
cached. `PokeApiPokemonSource` (the port adapter, D-032) has no `@Cacheable`: interactors call the port's
`default getByIdentifier`, which invokes `findByIdentifier` on `this` and would bypass the caching
proxy (self-invocation). Calls from the adapter into the client always cross a bean boundary.
**Alternatives considered:** `ConcurrentMapCache` (no dependency) has no TTL or eviction, so it
grows forever. Redis is an extra container and overkill for one instance. Persisting all of
PokeAPI in Postgres would mix the source with our local records.

## D-013 — API versioning: Spring Framework 7 native, path-segment strategy
**Status:** Superseded by D-029 · **Date:** 2026-10-05 · **Requirements:** TR-API-2, TR-OPT
**Decision:** Use Boot 4 / Spring Framework 7 first-class API versioning. URLs are
`/api/v{version}/...`, resolved by the framework (path segment 1). Handlers declare
`version = "1"`. Supported/default versions are configured once, and unsupported/missing versions →
400 `ErrorResponse`. Versioning lives only in `interfaces/rest`.
**Alternatives considered:** A request header (`API-Version`) keeps URLs stable and is
REST-purist, but it's invisible in the browser/Swagger and harder to demo or cache. A media-type
parameter is the most verbose for clients. A hard-coded `/api/v1` prefix isn't versioning at all,
because nothing can serve v2 side by side.
**Consequences:** v2 of an endpoint is a new handler plus mapper method. Use cases are untouched.

## D-014 — Remove Lombok
**Status:** Accepted · **Date:** 2026-10-06 · removes a scaffold dependency · **Requirements:** EV-3, D-001
**Decision:** Use records, explicit builders and constructors. Lombok would be forbidden in
`domain`/`application` anyway (D-001), and mixing styles across layers hurts readability during a
live code review.

## D-015 — Test tooling: Testcontainers (PostgreSQL), ArchUnit, JaCoCo
**Status:** Accepted · **Date:** 2026-10-06 · test-scope dependencies + Gradle plugin · **Requirements:** TR-UT, EV-2
**Decision:** Integration tests run against real Postgres via Testcontainers (`@ServiceConnection`).
Architecture rules run with ArchUnit, and coverage is reported with JaCoCo. `*IT` runs in a
separate `integrationTest` task wired into `check`.

## D-016 — OpenAPI UI via springdoc
**Status:** Accepted, timeboxed · **Date:** 2026-10-06 · **Requirements:** DL-1, TR-OPT
**Decision:** Try springdoc for `/swagger-ui.html` with a **30-minute timebox** (plan D.4): a
Boot-4.1-compatible release. If it doesn't work on the
first try, remove the dependency and rely on the `curl` examples in the README. No `api.http` file.

## D-017 — Monorepo: one Git repository at the root
**Status:** Accepted · **Date:** 2026-10-06 · the agent still asks before deleting `backend/.git` (plan 0.1) · **Requirements:** TR-GIT
**Context:** `backend/` was generated with its own (empty) `.git`, so the root repo would see it as
an embedded repository / gitlink.
**Decision:** One repository at the root (`backend/`, `frontend/`, `docs/`, `docker-compose.yml`).
Remove `backend/.git`, and add a root `.gitignore` (`.DS_Store`, `.idea/`, `*.iml`, `build/`,
`node_modules/`, `.env`).

## D-018 — Concurrent PokeAPI fan-out on virtual threads
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** US-01
**Decision:** A list page needs 2 PokeAPI calls per entry, so the adapter runs them concurrently
on virtual threads (Java 25), bounded by `pokeapi.max-concurrency`, with each entry cached. Page
size is capped at 50.
**Alternatives considered:** Sequential calls make the first load of 20 entries cost 40 round trips.
WebClient/reactive is a whole new paradigm for one adapter.

## D-019 — Containers: Dockerfile per app + root `docker-compose.yml`
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** DL-3
**Decision:** The backend is a multi-stage build (JDK 25 build → JRE 25 runtime, non-root user,
`curl` only for the healthcheck). The frontend is a multi-stage build (Node build → nginx serving
the SPA, with `/api` proxied to the backend). Compose runs `postgres`, `backend` and `frontend`,
each with a healthcheck and each starting only once the previous one is healthy.
`docker compose up --build` is the one-command demo. The app is on `http://localhost:3000`, the
API on `:8080`, and Postgres on host port **5433** (configurable via `POSTGRES_PORT`), so it never
clashes with a Postgres already running locally.
**Consequences:** The browser only talks to one origin, both in Docker (nginx proxy) and in
development (Vite proxy), so the backend needs **no CORS configuration**. Tests don't run inside
the image build, because the ITs need Docker. They run with `./gradlew check`.

## D-020 — Frontend routing: React Router
**Status:** Accepted · **Date:** 2026-10-06 · dependency · **Requirements:** FE-1, FE-3

## D-021 — Frontend server state: TanStack Query
**Status:** Accepted · **Date:** 2026-10-06 · dependency · **Requirements:** FE-4
**Decision:** All server state lives in the TanStack Query cache, and mutations invalidate precise
keys. There's no Redux/Zustand, because the only global client state is the session (a Context).
**Alternatives considered:** Hand-rolled `useEffect` fetching re-implements caching, dedup and race
handling badly, and that's the most common source of console warnings.

## D-022 — Frontend forms: react-hook-form + zod
**Status:** Rejected · **Date:** 2026-10-06 · **Requirements:** FE-2, US-04
**Proposal:** Use react-hook-form + zod for client-side validation that mirrors the backend limits.
**Why rejected:** There are only three small forms (login, register, edit). Two dependencies and a
schema that duplicates the domain rules aren't worth it for that.
**Instead:** Controlled inputs, plus a small pure `validate()` per form for required fields and
sizes. Server `fieldErrors` are mapped onto the fields, because the backend is the authority
(D-028).

## D-023 — Frontend tests: Vitest + React Testing Library + user-event + jsdom + MSW
**Status:** Accepted · **Date:** 2026-10-06 · dev dependencies · **Requirements:** TR-UT, FE-5

## D-024 — Access token held in memory + `sessionStorage`
**Status:** Accepted · **Date:** 2026-10-05 · **Requirements:** TR-AUTH-2
**Decision:** The token lives in memory, mirrored to `sessionStorage` so it survives a reload, and
it's cleared on logout or a 401.
**Trade-off (state it in the review):** Any JS storage is readable by XSS. An `httpOnly` cookie is
safer but brings back CSRF handling and cross-origin cookie configuration. For this scope, React's
default escaping, no `dangerouslySetInnerHTML`, and a short TTL are the mitigation, and the
production answer would be httpOnly cookie + CSRF token, or a BFF.

## D-025 — Language: English in the repo, Portuguese in the conversation
**Status:** Accepted · **Date:** 2026-10-05
**Decision:** All code, comments, docs, commit messages and UI copy are in English, since the
brief is in English and the repository is public. The AI assistant talks to the developer in pt-BR.

## D-026 — US-04 edits only the proprietary fields
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** US-04, TR-API-1, OV-5
**Context:** The brief asks for "update operations for any Pokemon currently stored" and
"attribute modification", without restricting which attributes. D-006 limits `PUT` to the
proprietary `CustomAttributes`. That's defensible, but with three editable fields a reviewer can call
the CRUD thin, so the interpretation needs an explicit, approved answer.
**Decision:** Keep D-006. `PUT /pokemon/{identifier}/local` replaces only the custom attributes; canonical data
comes only from PokeAPI, at sync time. The walkthrough states the interpretation and the reason
(D-006: a resync would otherwise clobber local edits, and "ours vs theirs" stays explicit).
**Alternatives considered:** Editing canonical fields directly blurs the line and makes resync
destructive. A per-field override layer (`overrides` applied on top of the profile) is expressive
but adds model, persistence and UI weight for little demo value.
**Consequences:** No model change.

## D-027 — Localized names keyed by language
**Status:** Rejected · **Date:** 2026-10-06 · **Requirements:** US-03.a
**Context:** The brief's "localized nomenclature" is modelled as one `localizedName` string. A
localized name without its language is ambiguous: is `"ピカチュウ"` Japanese, and which name is the
French one?
**Decision:** Replace `localizedName` with `localizedNames`: a map from a BCP 47 language tag (a new
`LanguageTag` VO, e.g. `ja`, `fr`, `pt-BR`) to a name, at most 10 entries, each name ≤ 100 chars,
violations → `InvalidCustomAttributesException` (Validation, 400).
**Alternatives considered:** Keep the single string: simplest, but ambiguous and easy to question.
A `localizedName` + `locale` pair: unambiguous, but only one language per Pokémon.
**Why rejected:** It would add a new VO, a child table, a new request/response shape and a
key–value editor in the frontend, plus a rule for which language the API displays. That's beyond
the brief, which only names localized nomenclature as an example ("use cases include …").
**Consequences:** `localizedName` stays a single free-text string. If asked, the answer is that the
brief's requirement is met, the ambiguity is known, and a language-keyed map is the next step.

## D-028 — The domain is the single validation authority
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** TR-BL-1, TR-BL-2, US-04.b
**Context:** The brief wants the business layer to encapsulate "all domain rules and data
validation procedures". Today validation is split: Bean Validation on request records, invariants
in domain VOs. The split is fine only if the edge never holds a rule the domain lacks. Right now it
does: `@Email` on `RegisterUserRequest` and the `Email` VO's "basic format" can accept or reject
different strings.
**Decision:** Every validation rule lives in the domain. Bean Validation on request records only
checks **shape** (required fields, types, collection and string sizes) for fast field-level 400s,
and every limit it uses references the domain constant (`@Size(max = CustomAttributes.MAX_TEXT_LENGTH)`)
instead of repeating a literal. No format rule exists only at the edge: `@Email` is dropped and the
`Email` VO decides.
**Alternatives considered:** Bean Validation inside the domain is forbidden by D-001. Edge-only
validation contradicts the brief.
**Consequences:** Applied on 2026-10-06 to `examples/rest.md` (request records: required-ness and
sizes from domain constants, no `@Email`) and to the validation section of `domain-model.md`. The
review answer is "the domain decides; the edge only gives earlier, field-specific feedback".

## D-029 — Plain `/api/v1` prefix; native API versioning parked
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** TR-API-2 · **Supersedes:** D-013
**Context:** Native versioning (D-013) isn't required by the brief. It adds configuration that
hasn't been verified on Boot 4.1, and it risked rejecting unversioned paths (`/error`,
`/actuator/health`). It's scope beyond the brief.
**Decision:** Controllers map under a plain `/api/v1` prefix. There's no `version` attribute and no
`spring.mvc.apiversion.*` configuration.
**Alternatives considered:** Keeping D-013. It's elegant and a good talking point, but it costs
setup and verification time for something the brief doesn't ask for.
**Consequences:** One less thing to verify, and the 0.4 versioning IT is gone. The native feature is
parked as a next step. Bumping later means a new `/api/v2` controller, or adopting D-013 then.

## D-030 — Product vision: one merged Pokémon resource; reads public, writes protected
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** OV-5, US-01…US-04, TR-API-1, TR-AUTH-3 · **Supersedes:** D-009
**Context:** The brief never states what the product is, only that the service retrieves from
PokeAPI, replicates locally, and lets users modify attributes. Splitting "the PokeAPI catalog" and
"our Pokédex" into two screens and two APIs made the product hard to explain and pushed a two-source
merge onto the frontend.
**Decision:**
- **The product.** A Pokémon catalog service. PokeAPI is the source of canonical data. A logged-in
  user syncs chosen Pokémon into the shared local database and maintains the proprietary fields
  (localized name, region, tags). Local data is the organization's, with no per-user ownership.
- **One resource, merged by the backend.** `GET /api/v1/pokemon` and `GET /api/v1/pokemon/{identifier}`
  return PokeAPI data merged with the local record (`displayName`, `synced`, `local`). Routes never
  say where data comes from.
- **Writes on a `/local` sub-resource.** `POST` (sync), `GET`, `PUT` (custom attributes) and
  `DELETE` on `/api/v1/pokemon/{identifier}/local`, so every verb means what it says.
- **Access.** Every read is public. Only `POST`/`PUT`/`DELETE` on `/local` and `/auth/me` need a
  token. No roles.
- **Frontend.** Two pages, list and detail. Sync, edit and remove live on the detail page.
**Alternatives considered:** Two resources (`/catalog` + `/pokemon` CRUD by UUID), which was the
earlier design: two lists for one concept, and the frontend would need two calls to show one
Pokémon. A per-user collection: a different product from the brief's "internal classification",
with ownership checks everywhere. Writes directly on `/pokemon/{id}`: a `DELETE` followed by a
`GET` that still returns 200 is incoherent.
**Consequences:** The merge is a use case concern (port + repository). The list merge costs one DB
query per page. If PokeAPI is down, even synced Pokémon return 503 on the merged reads
(`GET …/local` still works). Falling back to the snapshot is parked.

## D-031 — The local record stores a scalar snapshot, not the full profile
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** US-03, TR-DB-1, TR-DB-2 · **Supersedes:** part of D-006
**Context:** With merged reads (D-030), types, abilities, stats and the evolution chain are always
read from PokeAPI. Storing them locally would mean three more child tables, embeddables and
ordering rules, for data no screen reads from the database.
**Decision:** `local_pokemons` keeps a scalar `PokemonSnapshot` (number, name, category, height,
weight, sprite, artwork, description) plus the custom attributes. Tags are the only child table.
**Alternatives considered:** The full profile, which is more complete but triples the persistence
work for nothing visible. Only the custom attributes, which wouldn't be "persisting Pokémon data"
(US-03).
**Consequences:** US-03 is met with two tables. The snapshot is what a future offline fallback
would use.

## D-032 — Naming: no "Pokédex", no "catalog"; `PokemonSource` and `LocalPokemon`
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** EV-3, EV-5 · **Renames:** D-004
**Context:** "Pokédex" suggested a personal collection, and "catalog" meant only the PokeAPI half,
while the whole system is a catalog. Both made the model harder to explain.
**Decision:** Use the brief's own words. **PokeAPI** is the source (`PokemonSource` port,
`PokeApiPokemonSource` adapter, `PokemonSourceUnavailableException`). The **local database** holds
`LocalPokemon` records. **Sync** is the brief's "Data Synchronization". `PokemonNotFoundException`
means "not in PokeAPI", and `LocalPokemonNotFoundException` means "not synced".
**Consequences:** No change in behaviour. D-004's reasoning (a domain-owned port, an outage
exception in the port contract) is unchanged.

## D-033 — Vertical slices, authentication first; an unknown account on `/auth/me` is a 401
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** TR-AUTH-1..3, FE-1…FE-5, US-01…US-04
**Context:** The plan built the whole backend first and the whole frontend at the end. That puts
the integration risk (token handling, error shapes, proxying) and a graded criterion (the
frontend) at the end, and it means nothing is demoable until then. The developer also wanted
authentication first, because it's the most fiddly part and every later endpoint depends on it.
**Decision:**
- **Vertical slices.** After the foundation, each slice delivers one user action end to end,
  backend and frontend: sign up/in/out → browse the list → view a Pokémon → sync it → edit and
  remove → edits show up everywhere (the list/detail merge). TDD stays inward-out inside each slice.
- **Authentication is Slice 1.** The real `SecurityConfig` (with `/actuator/health` explicitly
  permitted) exists before any Pokémon endpoint, so each one is born with its 401 tests. Migrations
  follow: `V1` user accounts, `V2` local Pokémon, `V3` seed.
- **`/auth/me` with a valid token for an account that no longer exists → 401**
  (`UnknownAccountException`), not 404. It happens when the database is reset while a browser still
  holds a token. A 401 makes the frontend drop the session and go to sign in. There's no
  `UserAccountNotFoundException`.
**Alternatives considered:** Backend first, then frontend (the previous plan): simpler to describe,
but riskier at the end. Pokémon features before auth: faster first demo, but every protected
endpoint would get its security retrofitted.
**Consequences:** Frontend foundations (router, query client, test setup, HTTP client) land in
Slice 1. The list and detail come from PokeAPI only in Slices 2–3, and local data joins them in
Slices 4 and 6.

## D-037 — Frontend test matchers: @testing-library/jest-dom
**Status:** Accepted · **Date:** 2026-10-06 · dev dependency · **Requirements:** TR-UT, FE-5
**Context:** D-023 covers the runner and the rendering library, but not the DOM assertions.
**Decision:** Add `@testing-library/jest-dom`, loaded through its Vitest entry in the test setup.
Tests read as the user sees the page: `toBeDisabled`, `toHaveValue`, `toHaveAccessibleDescription`
for field errors.
**Alternatives considered:** Plain `expect`, with casts like `(el as HTMLButtonElement).disabled`:
one dependency less, but noisier tests and failure messages that say less.
**Consequences:** Dev-only, no production code. It's the standard companion of React Testing
Library.

## D-036 — Public routes ignore the bearer token
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** TR-AUTH-3, FE-4 · **Refines:** D-035
**Context:** The resource server validates any `Authorization` header it finds, even on a
`permitAll` route. Tokens live for an hour, so a browser still holding an expired one got a 401 on
the public Pokémon list, and even on login. Checked with a throwaway test before deciding.
**Decision:** The public routes are declared once, as a `RequestMatcher` in `SecurityConfig`. The
same matcher drives `permitAll` and a `BearerTokenResolver` that returns no token for those
routes, so public routes never read the header.
**Alternatives considered:** Leaving it to the frontend (send the token only on protected calls,
clear the session on 401). It works, but the rule would be hidden in one client, and any other
client would hit the same trap.
**Consequences:** A public route answers the same with or without a token, valid or not. A
protected route still rejects an invalid token with 401. `SecurityConfigIT` covers both, plus a
**real** issued token on a protected route, because `jwt()` from spring-security-test skips the
resolver.

## D-035 — The route policy is closed by default
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** TR-AUTH-3 · **Refines:** D-030
**Context:** D-030 says which routes are public (every read, register, login, health) and which are
protected (writes on `/local`, `/auth/me`). It doesn't say what happens to a route nobody listed.
The first example listed only the protected routes and ended in `anyRequest().permitAll()`.
**Decision:** List the **public** routes (`GET /api/v1/pokemon/**`, `POST` register and login,
`/actuator/health`) and end in `anyRequest().authenticated()`. `SecurityConfigIT` has a row for an
undeclared route that must answer 401.
**Alternatives considered:** Open by default, as in the first example: shorter, but a new write
endpoint left out of the list would be public, and no test would notice.
**Consequences:** A request to an unknown path without a token gets 401 instead of 404. With a
token it still gets the 404 `ErrorResponse`.

## D-034 — Frontend: UX first, UI later; design tokens and global components from day one
**Status:** Accepted · **Date:** 2026-10-06 · **Requirements:** FE-2, FE-4
**Context:** Each slice ships a screen. Polishing every screen as it's built spends time on looks
before the features exist, and repeated styling decisions drift between pages.
**Decision:** The slices build a functional, **usable** MVP (responsive, all async states,
accessible) with minimal neutral styling. A dedicated UI pass at the end adds the palette, the
typography and the visual polish. From day one, every style value is a design token in
`tokens.css`, and anything used twice is a global component in `shared/ui`. Pages only compose
those components.
**Alternatives considered:** Polishing per slice, which costs more time early and gives an
uneven look. A UI library, rejected in the frontend standard: a dependency, and it hides the
component organization the brief asks to see.
**Consequences:** The UI pass is mostly changing token values plus component styles, with no
page-by-page rework. Until then the app looks plain on purpose. Usability isn't deferred.

