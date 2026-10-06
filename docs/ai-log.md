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

### 2026-10-06 — The developer rejected a password message that talked about bytes
- **Context:** S1.1 wrote it, S1.10 put it on screen (TR-AUTH-1, FE-2).
- **AI proposed:** One `WeakPasswordException` for every password rule: "Password must have at
  least 8 characters, a letter and a digit, and at most 72 bytes".
- **Problem:** Seen in the browser, the developer called it nonsense: "72 bytes" is BCrypt's input
  limit, an implementation detail no user can count or act on, mixed into the message for the
  common mistake. The tests had pinned the text, so they passed; nobody had read it as a user.
- **Resolution:** Two exceptions: `WeakPasswordException` ("at least 8 characters, including a
  letter and a digit") and `PasswordTooLongException` ("Password is too long"). The sign-up form
  now shows the rule before the user types (`TextField` hint), so the error is rarely needed.
- **Lesson:** Error messages are UI copy. Read them where the user reads them, not only in an
  assertion.

### 2026-10-06 — returnTo: the obvious check missed a known bypass
- **Context:** S1.10, sending the user back after signing in (TR-AUTH, FE-3).
- **AI proposed:** Accept a `returnTo` that starts with `/` and not `//`.
- **Problem:** The AI remembered, while writing the tests, that browsers read `\` as `/`, so
  `/\evil.example` is protocol-relative too: an open redirect through the sign-in page. The first
  version let it through, and the new test failed on it.
- **Resolution:** `safeReturnTo` also refuses a leading `/\`, with its own red/green.
- **Lesson:** For security checks, write the attacker's inputs as tests, not just the happy path.

### 2026-10-06 — "Zero console errors" met a limit outside the app
- **Context:** S1.10, the manual browser pass on the Docker stack (FE-5).
- **AI proposed:** Treating the console guard in tests as proof of a clean console.
- **Problem:** In the real browser, a wrong password, a weak password and a duplicate email each
  left a "Failed to load resource: 4xx" line. Chrome writes those for every non-2xx response; the
  test guard can't see them because jsdom has no network panel.
- **Resolution:** Reported to the developer instead of hidden: the app writes nothing, the happy
  paths are clean, and the walkthrough FAQ explains the browser's own network lines.
- **Lesson:** Automated guards cover what their environment can see. The manual browser pass is
  still needed.

### 2026-10-06 — Small TDD slips the AI caught in its own frontend steps
- **Context:** S1.9, the HTTP client and the first `shared/ui` components (FE-4, TR-UT).
- **AI proposed:** A red step with two tests (one of them already passing), and a `Button` green
  step that also added `aria-busy`, which no test asked for.
- **Problem:** Both break the rules the project set for itself: one failing test per red, and
  only the code the test needs. A test that passes from the start proves nothing in a red step, and
  untested markup is a guess.
- **Resolution:** The passing test became its own `test:` step after the green, and `aria-busy`
  was removed before the step was saved.
- **Lesson:** The step snapshots make it cheap to fix a step before it becomes history.

### 2026-10-06 — The frontend toolchain: two checks before trusting it
- **Context:** S1.8, the frontend setup (FE-1, FE-5, TR-UT).
- **AI proposed:** Installing "the latest" of each approved library, and a console guard in the
  test setup that throws from `afterEach`.
- **Problem:** npm installed MSW 2.15 although the registry's latest is 3.0.2. Instead of forcing
  3.x, the AI asked npm why: Vitest's own mocker declares `msw ^2.4.9` as a peer, so 3.x would
  leave an invalid tree. Separately, a guard that only *looks* right is worthless, and React
  Router's API changed across 6/7/8 (`RouterProvider` now has a DOM entry with `flushSync`), so
  neither was taken on trust.
- **Resolution:** MSW 2.15 kept on purpose. The guard was proven with a throwaway test: a
  `console.error`, a `console.warn` and an unmocked request each fail, a quiet test passes. React
  Router's exports were read from the installed package before writing the router.
- **Lesson:** "Latest" isn't a version policy; a consistent dependency tree is. And a test guard
  needs its own proof that it can fail.

### 2026-10-06 — An ArchUnit rule written by the AI could never have checked anything
- **Context:** S1.7, removing the temporary `archRule.failOnEmptyShould=false` (OV-2).
- **AI proposed:** In the 1.1 rules, `noMethods().that().areAnnotatedWith(Transactional.class)
  .should().beDeclaredInClassesThat().resideOutsideOfPackage(...)`.
- **Problem:** With the temporary switch gone, ArchUnit failed it as "checked no classes". Its
  `that()` selected the violation itself, so in correct code it's always empty, and the switch had
  been hiding that since day one.
- **Resolution:** Rewritten to select every method outside the transaction adapter and forbid the
  annotation: same meaning, non-empty selection. A mutation (`@Transactional` on a JPA adapter
  method) fails it. The reference example was corrected.
- **Lesson:** A "temporary" relaxation of a quality gate hides defects for as long as it lives.
  Remove it at the first moment it's possible.

### 2026-10-06 — A probe test showed an expired token breaking public pages
- **Context:** S1.6, the auth endpoints, thinking ahead to the frontend (TR-AUTH-3, FE-4).
- **AI proposed:** Nothing wrong was generated yet. The AI suspected that a `permitAll` route still
  validates a bearer token, and wrote a throwaway test instead of assuming either way.
- **Problem:** Confirmed: an invalid token on `GET /api/v1/pokemon` answered 401. With one-hour
  tokens, the public list (and even login) would break for anyone whose token had expired. Then a
  second gap: every existing "authenticated" test used `jwt()` from spring-security-test, which
  bypasses the bearer token filter, so none of them could catch a resolver that ignored all tokens.
- **Resolution:** D-036, chosen by the developer over a frontend-only rule: one `PUBLIC_ROUTES`
  matcher for `permitAll` and a `BearerTokenResolver` that skips those routes. A test with a real
  token from `JwtTokenIssuer` went in first; a mutation (resolver always `null`) fails only that
  test.
- **Lesson:** Test helpers that fake authentication can skip the very code under test. Keep at
  least one test on the real path.

### 2026-10-06 — The reference security config was open by default, and half right on 401s
- **Context:** S1.5, the route policy and the 401 writer (TR-AUTH-3).
- **AI proposed:** The AI-written example: protect the listed writes, `anyRequest().permitAll()`,
  register the 401 writer in both `exceptionHandling` and `oauth2ResourceServer`, and add a 403
  writer "as a safety net".
- **Problem:** Before coding, the AI flagged that open-by-default makes any forgotten write endpoint
  public, and the developer chose closed by default. The tests then showed the rest: registering
  the entry point on the resource server alone already covers a missing token, while
  `exceptionHandling` alone misses invalid tokens (a mutation made exactly that test fail). And
  with no roles, nothing can produce a 403, so that writer would have been untested code.
- **Resolution:** D-035 (closed by default, with a test row for an undeclared route), one
  registration on the resource server, no 403 writer until roles exist. The examples and the
  backend standard were corrected.
- **Lesson:** Generated reference code carries plausible but unverified claims ("register it in
  both places"). One mutation is cheaper than trusting it.

### 2026-10-06 — Strict stubs hid a red step, so a mutation proved the test instead
- **Context:** S1.4, `AuthenticateUserInteractor` (TR-AUTH-2).
- **AI proposed:** The textbook order: "valid credentials get a token" first, then "a wrong
  password is refused" as its own red.
- **Problem:** Mockito's strict stubs fail a test whose stubs go unused. Stubbing
  `passwordHasher.matches(...)` in the happy path forced the first green to check the password
  already, so the wrong-password test could never be seen red. The AI also first stubbed
  `findByEmail → Optional.empty()` in the register test, which Mockito returns anyway; with strict
  stubs, that stub would have forced production code to call it. Caught before the green step.
- **Resolution:** The redundant stub was removed. The wrong-password test went in as a `test:`
  step, and was proven by mutation: deleting the `.filter(passwordHasher.matches…)` line turns it
  red.
- **Lesson:** When a test can't be seen failing for real, a deliberate mutation is the substitute.
  A test that was never red isn't evidence.

### 2026-10-06 — Adding the JWT decoder silently closed the health endpoint
- **Context:** S1.3, password hashing and JWT (TR-AUTH-2).
- **AI proposed:** Creating the `JwtEncoder`/`JwtDecoder` beans now and leaving the
  `SecurityConfig` for S1.5, as the plan said.
- **Problem:** `ApplicationHealthIT` went red with a 401. A `JwtDecoder` bean is enough for Spring
  Boot to activate the resource server's default filter chain, which authenticates every request,
  `/actuator/health` included. Docker's healthcheck would have failed with no code change in the
  security layer. Separately, the plan's "HS256 needs at least 32 bytes" was an unverified claim.
- **Resolution:** A minimal `SecurityConfig` (stateless, CSRF off, health permitted, the rest
  authenticated) moved forward from S1.5, which now only completes it. The 32-byte minimum was
  checked against Nimbus 10.9.1 itself: 31 bytes throw "The secret length must be at least 256
  bits", 32 pass. `JwtProperties` now refuses a shorter secret at startup instead of failing on the
  first login.
- **Lesson:** In Spring Boot, adding a bean can change behaviour far away from it. The test that
  guarded the healthcheck is what made this visible.

### 2026-10-06 — The error-handler tests caught three defects before any endpoint existed
- **Context:** Plan task 1.4, the global error handler (TR-ERR, US-04.b).
- **AI proposed:** (1) A `@WebMvcTest` with a test controller nested in the test class. (2) The
  catch-all `@ExceptionHandler(Exception.class)` from the reference example. (3) Relying on Bean
  Validation's default messages.
- **Problem:** (1) The first green run failed with an empty 404: Spring Boot deliberately doesn't
  component-scan classes nested in tests, so the probe controller never existed. That was a test
  bug, fixed before the red step was committed. (2) A test for a `GET` on a `POST`-only endpoint
  showed the catch-all turning Spring's own 405 and 415 into 500s. The reference example had the
  same bug. (3) A test sending `Accept-Language: pt-BR` got "não deve estar em branco" back.
  Validation messages followed the browser's language, while the rest of the API is English.
  Checking the property name in Boot 4.1's configuration metadata also showed that
  `spring.mvc.locale` is deprecated in favour of `spring.web.locale`.
- **Resolution:** `@Import` for the probe controller. The catch-all now keeps the status of any
  `org.springframework.web.ErrorResponse`. The locale is fixed to English. The example and the
  backend standard were updated so later code doesn't copy the bug.
- **Lesson:** Test the error paths a user can actually trigger (wrong verb, wrong media type,
  another browser language), not only the ones the code was written for. A catch-all is exactly
  where those get lost.

### 2026-10-06 — The developer reshaped the plan into vertical slices
- **Context:** Planning the implementation order after the foundation (all IDs, D-033).
- **AI proposed (earlier):** A layer-by-layer plan: the whole backend, then the whole frontend.
  When asked, it suggested moving authentication earlier, but kept the backend-then-frontend shape.
- **Problem:** The developer described the work as user actions (sign in → see the list → see a
  Pokémon → edit it → see the edit everywhere). The AI's plan left the frontend, a graded
  criterion, and all the integration risk to the end, with nothing demoable before.
- **Resolution:** The plan was rewritten as six vertical slices, each runnable end to end. The AI
  added the two steps the developer's list skipped but the brief requires (sign up, and sync before
  edit) and turned the last step into the list/detail merge.
- **Lesson:** Plan in user actions, not in layers. A layer plan looks complete on paper and hides
  the moment nothing works yet.

### 2026-10-06 — An ArchUnit method in the reference examples didn't exist
- **Context:** Writing `LayeredArchitectureTest` from `docs/examples/tests.md` (plan 1.1, OV-2).
- **AI proposed (earlier, in the examples it wrote):** `noClasses()…should().beAnnotatedWith(Transactional.class)
  .orShould().containAnyMethodsThat(annotatedWith(Transactional.class))`.
- **Problem:** The compiler rejected it: `ClassesShould` in ArchUnit 1.5.1 has no
  `containAnyMethodsThat`, which `javap` on the jar confirmed. The API was plausible-sounding and
  invented. Every agent copying the example would have hit the same wall.
- **Resolution:** Two rules using the real API: classes annotated outside the transaction package,
  and `noMethods().that().areAnnotatedWith(…).should().beDeclaredInClassesThat()…`. The example was
  fixed too. The whole rule set was then proven with a throwaway violating class: the three
  expected rules failed, and nothing else did.
- **Lesson:** Reference docs written by an AI need the same compile-and-run check as its code. A
  fluent API makes invented method names look especially natural.

### 2026-10-06 — Infrastructure: four assumptions checked, two of them wrong
- **Context:** Making the stack run end to end before any feature code: Gradle, Docker, compose,
  first IT (plan 0.2–0.5, DL-3, TR-TEST).
- **AI proposed (earlier):** (1) The docs said `RestClient` needs no extra dependency on Boot 4.
  (2) The AI expected the first `/actuator/health` test to be red (401), because Spring Security
  locks everything by default. (3) A build config that would just work with no unit tests yet.
  (4) Postgres published on host port 5432.
- **Problem:** Each one was checked against the real thing. (1) Maven Central shows Boot 4.1.1
  split `spring-boot-starter-restclient(-test)` out: the class is in `spring-web`, but the
  `RestClient.Builder` bean and `@RestClientTest` are not. The docs were wrong. (2) The test was
  green on the first run. Boot's actuator security already permits the health endpoint, so the
  planned `SecurityConfig` wasn't needed yet. A test that passes first time proves nothing, so it
  was then broken on purpose to prove the `integrationTest` task really runs it. (3) Gradle 9
  failed `test` with "No tests found". (4) Port 5432 was taken by another project's container on
  this machine.
- **Resolution:** (1) The docs were fixed, and the starters are noted for plan 2.4. (2) No security
  code yet. The finding is recorded, and the IT stays as the guard. (3) The two `failOn…` checks
  are off on `test` only, with a comment and a plan item (1.1) to switch them back on. (4) Postgres
  is published on 5433 (configurable). The other project's container was left untouched.
- **Lesson:** Version-specific framework behaviour is where an AI is most confidently wrong. Check
  coordinates at the source, run the test before believing it, and make a passing test fail once.

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
