# AI Collaboration Log

This project treats **fluency with GenAI tools and critical thinking about AI-generated code**
(EV-6). This log is the evidence. It records the moments where AI output was **checked, caught,
corrected or rejected**, and why. It's raw material for the walkthrough and for code review.

## When to add an entry (the agent does this proactively)

- The developer corrects or rejects something the AI produced (code, design, an interpretation).
- A test, the compiler, ArchUnit or a manual check catches a defect in AI-written code.
- The AI catches and fixes its own mistake, or abandons its first approach.
- The AI guessed at something external (a PokeAPI field, a Spring Boot 4 property or coordinate),
  and verification showed it was wrong. Or it showed it was right, which is still worth one line if
  it was risky.
- A non-obvious prompt or workflow technique worked especially well.

Keep entries short and factual. Newest first.

## Entry template

```
### YYYY-MM-DD — short title
- **Context:** what was being built (requirement IDs).
- **AI proposed:** the suggestion, in one or two lines, or a short snippet.
- **Problem:** what was wrong, or risky, and how it was detected (test / review / docs / ArchUnit).
- **Resolution:** what was done instead. Link a commit or a decision (D-0NN).
- **Lesson:** the reusable takeaway (one line).
```

---

## Entries

### 2026-10-06 — The product was unclear until the developer reframed it
- **Context:** Scope review against the brief (OV-5, US-01…US-04, TR-AUTH-3, FE-3).
- **AI proposed (earlier):** Two separate concepts: a "catalog" (PokeAPI, browsed through
  `/catalog`) and a "local Pokédex" (CRUD by UUID on `/pokemon`, login required even to read), with
  two screens and a frontend that would call both. It then answered the developer's questions in
  terms that made it worse ("Pokédex", "catalog", a per-user option), and the developer said
  plainly that the AI was confusing them.
- **Problem:** The brief never states the product. The AI's vocabulary ("Pokédex" sounds personal,
  "catalog" meant only half the system) hid a simple design behind two parallel ones, and the
  developer couldn't explain it, which would have shown in every walkthrough.
- **Resolution:** The developer reframed it in concrete terms, then proposed the simpler design
  themselves: one list page and one detail page, with the backend merging PokeAPI and local data in
  a single request, and routes that don't reveal the data source. The AI added the `/local`
  sub-resource for writes (so `DELETE` then `GET` stays coherent) and the scalar snapshot (D-030,
  D-031, D-032). The product vision now opens `domain-model.md`.
- **Lesson:** When the human keeps asking "but what is this for?", stop adding options and restate
  the product in their words. A design that can't be explained in two sentences isn't finished,
  however correct each part is.

### 2026-10-06 — Third review: no cut line in the plan, plus three AI-introduced inconsistencies
- **Context:** The developer asked for a review only (no edits) of the updated agent docs against
  the brief (all IDs, EV-4, EV-6).
- **AI proposed (earlier, in the docs it wrote):** An ~40-task plan with no priorities and no cut line.
  An entity-mapper rule naming only `toEntity`/`toDomain` while the example needs `copyInto` for
  D-011 to work. A fixture task for alternate forms that had already been parked. A walkthrough
  agenda that added up to more than its own time budget.
- **Problem:** Found by reading the docs against each other. An agent following the standard alone
  would have generated a `save` that defeats optimistic locking. The plan treated every task as
  equally important, with nothing marking what the brief actually requires. Path-segment API versioning
  could also reject unversioned paths (`/error`, `/actuator/health`), which nobody has verified yet.
- **Resolution:** All open decisions were resolved in one pass (D-022 and D-027 rejected, the rest
  accepted, D-016 timeboxed). Resync, `If-Match` and the frontend extras were parked. `plan.md`
  got an ordered cut list and a "never cut" list. The versioning check became the first IT in 0.4
  (later moot, D-029). The GenAI case study was moved before the frontend.
- **Lesson:** Ask an AI to plan and it will produce the complete plan, not the one that fits the
  real constraints. The constraints have to be in the prompt, and the cut order has to be decided
  up front, not under pressure.

### 2026-10-06 — Second doc review against the brief; the developer cut the AI's scope creep
- **Context:** Another line-by-line review of the agent docs against the brief (US-01, US-04,
  TR-UT, TR-DAL, DL-2, FE-3, EV-5).
- **AI proposed (earlier, in the docs it wrote):** `RawPassword` limited to "72 chars"; a coverage
  report fed only by the `test` task while controller and repository tests are `*IT`; a demo seed
  of "~10 well-known Pokémon" next to a demo script and fixtures that both sync Pikachu; a frontend
  that sends the bearer token on every call; two different rules for picking the description.
- **Problem:** Checked against sources, not memory. Spring Security 7.1.1 `BCrypt.hashpw` throws
  above 72 **bytes**, so a password of accented chars would pass the VO and return a 500.
  `BearerTokenAuthenticationFilter` (7.1.0) rejects an invalid token before the `permitAll` rules,
  so an expired token in `sessionStorage` would break the public catalog. The JaCoCo default
  would show the adapters near 0%, which undersells the brief's "thorough unit test coverage". A
  seeded Pikachu would turn demo step 4 into a 409. Then the AI started proposing three new
  decisions (a species-based catalog, a separate Flyway location for demo data, a second cache
  layer), and the developer stopped it: stick to the brief and the existing instructions.
- **Resolution:** Only the cheap fixes that serve the brief or prevent a demo bug were applied:
  a 72-byte rule in the model and examples, merged coverage data wired into `check`, Pikachu
  excluded from the seed, a token sent only on protected calls and dropped once expired, the four
  US-01 card fields as acceptance criteria for plan 6.4, one description rule, and a TR-DAL
  answer in the walkthrough. Everything else went to `plan.md` → "Parked".
- **Lesson:** A review agent tends to turn every finding into new design work. Sort findings by
  "does this break the brief or the demo?" and park the rest, because a plan you can't execute in
  the time available is worth less than a smaller one you finish.

### 2026-10-05 — Reviewing the AI-written agent docs against the brief caught real defects
- **Context:** The developer asked for a line-by-line review of the agent docs against the
  project brief (all requirement IDs, EV-1…EV-6).
- **AI proposed (earlier, in the docs it wrote):** `@Cacheable` on `PokeApiPokemonCatalog.findByIdentifier`;
  a catalog example that called `Optional.get()`; `SecurityConfig` (infrastructure) writing
  `ErrorResponse` (interfaces); a route policy in `standards/backend.md` with public local reads;
  two different lists of cache names; naming examples for classes that don't exist; a note saying
  the examples were outdated after they had been rewritten; a walkthrough outline with no frontend.
- **Problem:** Found by reading the docs against the brief and against each other. The cache would
  never hit for detail or sync, because interactors call the port's `default getByIdentifier`,
  which invokes `findByIdentifier` on `this` and bypasses Spring's proxy (self-invocation). The
  `Optional.get()` would have failed the project's own ArchUnit rule. The layering example broke
  the doc's own "infrastructure and interfaces know nothing about each other". The route policy
  contradicted D-009. The resync/delete contract omitted statuses the DoD requires tests for. The
  brief asks for frontend best practices to be shown, and the outline skipped them.
- **Resolution:** A dedicated cached `PokeApiClient` (D-012 updated) and a cache test that goes
  through `getByIdentifier`. The 401/403 writers moved to `interfaces/rest/security/`, plus a new
  ArchUnit rule `infrastructure_must_not_depend_on_interfaces`. The route policy, cache names,
  naming tables, reference list and contract were aligned. A frontend block and a data-model slide
  were added to the walkthrough. The interpretation questions became Proposed decisions D-026…D-028.
- **Lesson:** AI-written specs need the same review as AI-written code. A plausible annotation in
  an example (`@Cacheable` on the obvious method) can encode a framework bug that every later
  generation would copy.

### 2026-10-05 — Use cases annotated with `@Service` contradict strict Clean Architecture
- **Context:** Setting up this repository's agent instructions from a personal Spring standard
  (OV-2, EV-1).
- **AI proposed:** Carrying over the standard as-is, with use cases as `@Service` classes and
  mappers as `@Component`.
- **Problem:** The developer pointed out that this makes the use case layer depend on Spring. That
  breaks the Dependency Rule, which the brief requires.
- **Resolution:** D-001/D-003. `application` became framework-free, enforced by an ArchUnit
  allowlist, with input-port interfaces + `*Interactor` implementations wired in a
  `UseCaseConfig` composition root. The examples and the standard were rewritten to match.
- **Lesson:** Pragmatic team conventions and the textbook pattern a strict reading asks for can
  differ. Make the strictness explicit and enforce it mechanically.

### 2026-10-05 — Reference examples rewritten for the real domain
- **Context:** The agent docs initially reused generic `Order` examples from the personal standard.
- **Problem:** The developer asked for examples in the project's own domain. Generic examples make
  the AI pattern-match the wrong model and leave dead concepts behind (MCP, event handlers).
- **Resolution:** `docs/examples/` was rewritten against `Pokemon`, `PokemonCatalog`, and the auth
  ports, and `docs/domain-model.md` is now the single source for the model.
- **Lesson:** Give the agent context in the actual domain. Every irrelevant example is a chance to
  generate irrelevant code.
