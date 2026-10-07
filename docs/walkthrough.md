# Project Walkthrough

A guided tour of the project, for demos and code review: what it does, how it's built, and why.
Keep it **clear and concise**. **Backend and frontend** practices each get their own section. Every
claim should point at something concrete: a file, a test, a row in
[`requirements.md`](requirements.md).

> Completed in the plan's Walkthrough section. Keep it as an outline, not a script to read aloud.

## Agenda (about 25 minutes, then questions)

1. **Context (1 min).** The product in one sentence: a Pokémon catalog backed by
   PokeAPI, where the team syncs chosen Pokémon into a local database and enriches them with its
   own fields ([`domain-model.md` → Product vision](domain-model.md#product-vision)).
2. **User stories → design (2 min).** US-01…US-04 mapped to two pages and one resource: list and
   detail read merged data, and sync/edit/remove go to `/pokemon/{identifier}/local` (D-030).
   State the interpretations: category = genus, skills = abilities (D-010), the brief's three
   example proprietary fields, and why US-04 edits only those (D-026).
3. **Architecture (4 min).**
   - The layer diagram and the Dependency Rule. `domain` and `application` are framework-free (D-001).
   - Input ports + interactors + composition root (D-003), and why there's no presenter (D-002).
   - Output ports: repositories, `PokemonRepository`, `UnitOfWork`, `PasswordHasher`, `TokenIssuer`.
   - The merge: a use case combines the PokeAPI port and the repository. `displayName` is a domain
     rule on `LocalPokemon`.
   - **Data model** (TR-DB-1/2): `local_pokemons` (the Pokédex number + the custom attributes, D-039)
     with its tags table, and `user_accounts` as the user-management collection. Unique keys,
     `@Version`, Flyway-owned.
   - Show `LayeredArchitectureTest`: the rules are enforced, not just written down.
4. **Key technical decisions (3 min).** No remote call inside a transaction. Caching and concurrent
   fan-out for the PokeAPI list (D-012/D-018), including why the cache sits on a separate client
   bean. Optimistic locking (D-011). Uniform errors and the 400 split (D-007/D-028). JWT auth, with
   every read public and only writes protected (D-008/D-030).
5. **Frontend (3 min).** The brief's frontend criteria.
   - Feature-first structure (`app → features → shared`), with pages composing, hooks fetching and
     components rendering ([`standards/frontend.md`](standards/frontend.md)).
   - Two pages. The backend merges the data, so the frontend never stitches two sources together.
   - State split: server state in TanStack Query, session in `AuthContext`, the page number in the
     URL, form and UI state local. Why there's no Redux (D-021).
   - User-centred UX: loading/empty/error/success on every async view, field errors from the API,
     confirmation on delete, accessibility.
   - Responsiveness in both senses: the layout at 360/768/1280 px, and perceived speed (skeletons,
     cached PokeAPI data, no layout shift).
   - Zero console warnings: `StrictMode` on, plus the test guard that fails on any warning.
6. **Testing & TDD (2 min).** The inward-out cycle, one example traced from domain test → interactor
   test → IT, the coverage report, the frontend tests (RTL + MSW), and the test-first commits in the
   history.
7. **Live demo (5 min).** See the script below.
8. **AI-assisted development (3 min).** How the agent was set up (`AGENTS.md` + standards +
   decision log), two or three entries from [`ai-log.md`](ai-log.md) where AI output was corrected,
   and the task-API case study ([`genai-case-study.md`](genai-case-study.md)).
9. **Limitations & next steps (1 min).** From `plan.md` → "Parked".

## Demo script

1. `docker compose up --build` is already running, so show the containers healthy.
2. Logged out: browse the list. Seeded Pokémon already show their localized name and a "synced"
   badge. Paginate, open Eevee (branching evolution), and show a cached second load.
3. Open Pikachu (deliberately not in the seed): no local data. "Log in to sync" → login with the
   demo credentials from the README → back on Pikachu.
4. Sync → 201, and the local section appears. (Optional: `curl` the same sync → 409.)
5. Edit: localized name "Pikachu BR", a region and tags. Save, and the title changes, with "pikachu"
   underneath. Show a validation error (an invalid tag → 400 with the field message). Back to the
   list: the card shows the new name.
6. Remove with confirmation, and the detail goes back to "not synced".
7. DevTools open throughout: **an empty console**. Resize to mobile width.
8. Optional: Swagger UI, and `curl` showing malformed JSON → 400, `PUT` on a Pokémon that isn't
   synced → 404, a write without a token → 401.

## Design FAQ (a short answer + where to look)

- Why is PokeAPI a repository? → To the domain it is just where Pokémon are read from; it doesn't
  know or care that it's an HTTP API (`domain-model.md` → Pokémon repository). `PokemonRepository`
  has no `save` because the canonical data isn't ours to change; the interface says so. The
  developer chose this over a separate `*Source` name: one pattern for every data port.
- What happens when the database or PokeAPI is down? → Same answer for both: each repository
  throws its own `DataUnavailableException` subclass (the log says which, and why), and the API
  answers one 503 `DATA_UNAVAILABLE`. `DatabaseUnavailableIT` stops PostgreSQL for real; Hikari
  gives up after 3s instead of 30s.
- Why are `PokeApiClient` and `PokeApiPokemonRepository` two classes? → `@Cacheable` works through a
  Spring proxy, which only sees calls coming from outside the bean. Merged, `findAll` would call
  `this.fetchPokemon(...)` and the cache would never hit, silently. Split, every call crosses a
  bean boundary (D-012); `PokeApiPokemonRepositoryCacheTest` proves it, and fails if the annotation
  goes. It also keeps each test simple: HTTP against a mock server, concurrency with the client
  mocked.
- Why interfaces for use cases? Isn't that over-engineering? → D-003. Controllers depend on
  abstractions, and the interfaces cost nothing in the IDE.
- Why no `@Transactional` on use cases? → D-001 + `UnitOfWork`. Show `SpringUnitOfWork`.
- How do you guarantee the domain doesn't depend on Spring? → The ArchUnit allowlist. Adding a
  Spring import to `domain` fails the build.
- What happens if PokeAPI is down? → `PokemonDataUnavailableException` → 503 on the merged reads;
  `GET …/local` by number still works, and the cache softens it. Serving synced Pokémon offline
  would need a copy of PokeAPI's data, which D-039 dropped on purpose.
- Two users edit the same Pokémon at once? → `@Version` → 409 (D-011). Next step: expose the
  version and require `If-Match`, so a stale browser tab also gets a 409.
- Two users sync the same Pokémon at once? → The unique constraint, translated to 409. The
  transaction alone wouldn't stop it.
- Why does the list make so many PokeAPI calls? → PokeAPI's list endpoint returns only names, so
  fan-out + cache + concurrency (D-018). The local data for the page is one query.
- Why one merged endpoint instead of the frontend calling two? → D-030: the frontend shouldn't know
  where data comes from, and one request is one consistent view.
- Why do writes go to `/local`? → So `DELETE` then `GET` stays coherent: the Pokémon still exists in
  PokeAPI, only the local record is gone (D-030).
- Why does the database keep only some fields? → D-031: types, stats and evolution are always read
  from PokeAPI, so storing them would be persistence work no screen uses.
- Why is the cache on `PokeApiClient` and not on the source adapter? → D-012: the port's `default
  getByIdentifier` calls `findByIdentifier` on `this`, which bypasses Spring's proxy. Show the
  cache test that goes through `getByIdentifier`.
- The brief says "update any Pokémon". Why can't I edit the name? → D-026: canonical data is
  PokeAPI's and comes from the sync; US-04 edits what we own. The localized name *is* the name
  shown, without touching the original. Mixing the two would make any future refresh from PokeAPI
  clobber local edits.
- Why is the local data shared and not per user? → D-030: the brief's fields are organizational
  ("internal classification tags"), and users are an "auxiliary" API for protected routes. A
  per-user collection is a different product.
- Why is `localizedName` a single string, without a language? → D-027: the brief names localized
  nomenclature as an example. A language-keyed map is the next step, and the ambiguity is known.
- Where does validation live, the controller or the domain? → D-028: the domain decides; Bean
  Validation only gives earlier, field-specific feedback, using the domain's limits.
- Why TanStack Query and no Redux? → D-021: the only global client state is the session; server
  state needs caching and invalidation, not a reducer.
- How do you know there are no console warnings? → The test setup fails any test that logs a
  warning, plus a manual DevTools pass on every page before the demo.
- Why JWT in `sessionStorage`? → D-024 trade-off, and what production would use instead.
- How did you validate AI-generated code? → The tests-first workflow, ArchUnit, verifying external
  facts against docs, and [`ai-log.md`](ai-log.md) entries.
- The brief says the data layer is "the foundation for the API controllers". Why don't controllers
  call repositories? → Clean Architecture: controllers → use cases → repository ports, and
  `infrastructure/persistence` implements those ports. The data layer is still the foundation, one
  boundary further in (TR-DAL, D-001).
- The console shows "Failed to load resource: 401" after a wrong password. Isn't that a console
  error? → Chrome logs every non-2xx response by itself; no page code can suppress it. The app
  writes nothing to the console (the test guard fails any test that does), and the happy paths are
  clean. Returning 200 for a failed login to hide the line would break the API contract.
- What would you do next? → `plan.md` "Parked".
