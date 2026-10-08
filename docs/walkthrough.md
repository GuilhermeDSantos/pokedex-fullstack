# Project Walkthrough

A guided tour of the project, for demos and code review: what it does, how it's built, and why.
Keep it **clear and concise**. **Backend and frontend** practices each get their own section. Every
claim should point at something concrete: a file, a test, a row in
[`requirements.md`](requirements.md).

Keep it as an outline, not a script to read aloud.

## Agenda (about 25 minutes, then questions)

1. **Context (1 min).** The product in one sentence: a Pokémon catalog backed by
   PokeAPI, where the team syncs chosen Pokémon into a local database and enriches them with its
   own fields ([`domain-model.md` → Product vision](domain-model.md#product-vision)).
2. **User stories → design (2 min).** US-01…US-04 mapped to two pages and one resource: list and
   detail read merged data, and sync/edit/remove go to `/pokemon/{number}/local` (D-030, D-040).
   Walk the [table below](#user-stories--what-to-show).
   State the interpretations: category = genus, skills = abilities (D-010), the brief's three
   example proprietary fields, and why US-04 edits only those (D-026).
3. **Architecture (4 min).**
   - The layer diagram and the Dependency Rule. `domain` and `application` are framework-free (D-001).
   - Input ports + interactors + composition root (D-003), and why there's no presenter (D-002).
   - Output ports: repositories, `PokemonRepository`, `UnitOfWork`, `PasswordHasher`, `TokenIssuer`.
   - The merge: a use case combines the PokeAPI port and the repository; the list page asks for
     its 20 records in one query, tags included (a join fetch, proven by counting statements).
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
8. **AI-assisted development (3 min).**
   - The roles: I directed and decided (architecture and its strictness, the rules in `AGENTS.md`
     and the standards, product vision, scope, the slice order); the agent wrote most of the code
     under those rules, test-first, and nothing was committed before I had read it.
   - Two or three entries from [`ai-log.md`](ai-log.md) where AI output was corrected (PokeAPI as a
     repository, dropping the stored name, `/local` by number, the screen showing one Pokémon).
   - The task-API case study ([`genai-case-study.md`](genai-case-study.md)): the spec-first prompt,
     the blind first run I stopped, the run under my rules, and the defects the checks caught.
9. **Limitations & next steps (1 min).** From `plan.md` → "Parked".

## User stories → what to show

| Story | What it asks | API | Screen | Demo step |
|---|---|---|---|---|
| US-01 | Browse, paginated: sprite, category, mass, skills (+ cache) | `GET /pokemon?page=&size=` | List: cards, our localized name under the name | 2 |
| US-02 | Detail: image, stats, description, evolutionary lineage | `GET /pokemon/{name or number}` | Detail: artwork, facts, stats, evolution tree (Eevee branches) | 2, 3 |
| US-03 | Sync into the local database, for our own fields | `POST /pokemon/{number}/local` → 201, 409 if already synced | "Sync to local database" | 4 |
| US-04 | Update the local record; 404 missing, 400 malformed, more defensive logic | `PUT /pokemon/{number}/local` → 200, 400, 404, 409 | Edit dialog: localized name, region, tags | 5, 8 |
| CRUD-D | Remove the local record | `DELETE /pokemon/{number}/local` → 204, 404 | Remove, with confirmation | 6 |
| TR-AUTH | Registration, authentication, public vs protected routes | `POST /auth/register`, `POST /auth/login`, `GET /auth/me` | Sign in / Create account; visitors see no controls | 3 |

## Demo script

1. `docker compose up --build` is already running, so show the containers healthy.
2. Logged out: browse the list. Seeded Pokémon already show their (French) localized name under the
   name.
   Paginate, open Eevee (branching evolution), and show a cached second load.
3. Open Pikachu (deliberately not in the seed): no local data, and no controls for a visitor. Sign
   in from the header with the demo account (`demo@pokemon.com`, password in the README) → back on
   Pikachu, now with "Sync to local database".
4. Sync → 201: the page is the same Pokémon, now ours too (no fields set yet). (Optional: `curl`
   the same sync → 409.)
5. Edit: localized name "Pikachu BR", a region and tags. Save: "Pikachu BR" appears under the
   title, the region joins the facts, and the tags close the page. Show the validation: an invalid
   tag (`bad tag!`) → 400 with the domain's message above the form, the typed values kept. Back to
   the list: the card already shows the new name.
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
  gives up after 3s instead of 30s. With PokeAPI down, our own data stays readable and editable on
  `/pokemon/{number}/local` (D-040), and the cache softens it; serving synced Pokémon offline would
  need a copy of PokeAPI's data, which D-039 dropped on purpose.
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
- Why does the database keep only the Pokédex number and our fields? → D-039: PokeAPI stays the
  source of truth for everything else, the name included, so nothing local can go stale and a
  renamed Pokémon needs no migration. The earlier snapshot (D-031) was dropped for that reason.
- Why do the `/local` routes take only the number? → D-040: the record is keyed by it, and the
  detail page already has it. Get, edit and remove never call PokeAPI, so editing our data works
  even while PokeAPI is down; a name there is a 400.
- The brief says "update any Pokémon". Why can't I edit the name? → D-026: canonical data is
  PokeAPI's; US-04 edits what we own. The name is always the title, and our localized name sits
  under it, on the detail and the list alike. Editing PokeAPI's name would be a copy that goes
  stale.
- Why did `displayName` disappear from the API? → Once every screen titled a Pokémon by its name
  and showed the localized one underneath, nothing read it; the list carries `localizedName`
  instead. Unused contract is dead code.
- After an edit, how is the list already up to date? → Every write calls `refreshOurData`: the
  detail on screen refetches, and the cached list pages refetch right away too (`refetchType:
  'all'`). A test edits, waits for that refetch, goes back and checks the card with no wait.
- How does the demo data get there, and is it safe to rerun? → A Flyway migration (`V3`): the demo
  account, hashed by the app's own `BCryptPasswordHasher`, and ten synced Pokémon with names from
  PokeAPI. `ON CONFLICT DO NOTHING` keeps it from failing on a database that already synced some of
  them; `ApplicationContextIT` signs the demo account in.
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
