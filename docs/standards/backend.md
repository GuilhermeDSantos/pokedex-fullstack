# Backend Standard — Java + Spring Boot, Clean Architecture + DDD

Binding rules for everything under `backend/`. The root [`AGENTS.md`](../../AGENTS.md) holds the
universal rules (language, determinism, TDD, workflow); this file is the concrete Java form of them.
Worked code for every pattern below is in [`../examples/`](../examples/) — read it instead of
inventing patterns. The project's actual model and API contract are in
[`../domain-model.md`](../domain-model.md).

Stack (as scaffolded): **Java 25 · Spring Boot 4.1 · Gradle 9 (Groovy DSL) · PostgreSQL · Flyway**.
Base package: `dev.guilhermeds.backend`.

---

## Architecture

Clean Architecture + DDD. Dependencies point inward only.

```
interfaces     → application → domain
infrastructure → application → domain
```

Domain knows nothing outside itself. Application knows only domain. Infrastructure and interfaces
know everything inward and nothing about each other.

### Package structure

```
dev.guilhermeds.backend
├── domain
│   ├── model           # Entities, Value Objects, Aggregates
│   ├── repository      # Repository interfaces (ports) — persistence
│   ├── source          # PokemonSource port + its read types (PokeAPI, seen from the domain)
│   ├── pagination      # Page / PageRequest
│   ├── service         # Domain Services (plain objects, not beans)
│   └── exception       # DomainException + categories + concrete exceptions
├── application       # PURE JAVA — no Spring, no Jakarta, no annotations from any framework
│   ├── usecase         # Input ports ({Verb}{Name}UseCase interfaces) + their {Verb}{Name}Interactor
│   ├── service         # Orchestration shared by 2+ interactors (only once it's real)
│   ├── port            # Output ports for orchestration: UnitOfWork, PasswordHasher, TokenIssuer
│   ├── query           # Read-model interfaces (cross-aggregate screens only)
│   ├── dto             # Input / Output records
│   └── mapper          # Input DTO → domain object mappers
├── infrastructure
│   ├── persistence
│   │   ├── entity      # JPA @Entity classes
│   │   ├── repository  # Spring Data interfaces + Jpa{Name}Repository adapters
│   │   ├── query       # Read-model implementations
│   │   └── mapper      # JPA entity ↔ domain mappers
│   ├── transaction     # SpringUnitOfWork — the only place @Transactional may appear
│   ├── security        # PasswordHasher / TokenIssuer adapters (BCrypt, JWT)
│   ├── config          # Composition root (UseCaseConfig wires interactors/mappers as @Bean),
│   │                   # @ConfigurationProperties, SecurityFilterChain, Clock, cache
│   └── external
│       └── pokeapi     # PokeApiPokemonSource (port adapter), PokeApiClient (cached HTTP), PokeAPI JSON records
└── interfaces
    └── rest
        ├── controller  # @RestController + GlobalExceptionHandler
        ├── request     # HTTP request bodies (Bean Validation lives here)
        ├── response    # HTTP response bodies
        ├── mapper      # Application DTO ↔ HTTP request/response
        └── security    # AuthenticationEntryPoint / AccessDeniedHandler that write ErrorResponse
```

Don't pre-create empty packages. A category appears the first time a class needs it.

### Strict Clean Architecture: the framework stops at the adapters

The brief requires Clean Architecture, so this project follows it **strictly** — stricter
than a typical Spring codebase:

- **`domain` and `application` are both framework-free.** No `@Service`, `@Component`,
  `@Transactional`, `@Value`, Jakarta Validation, Jackson, slf4j or Lombok anywhere in them. Their
  only allowed dependencies are `java..` and inner layers — an ArchUnit allowlist enforces it for
  both. You could lift `domain` + `application` into a plain Java library and they would compile.
- **Input ports.** Every use case is an interface in `application/usecase/` named
  `{Verb}{Name}UseCase`, with a single `execute(...)` method. Its implementation is a plain class
  `{Verb}{Name}Interactor implements {Verb}{Name}UseCase` in the same package. Controllers depend on
  the **interface** only, never on an `*Interactor`. That's enforced by ArchUnit.
- **Output ports.** Everything the interactor needs from the outside world is an interface declared
  inward: repositories and `PokemonSource` in `domain/`, and `UnitOfWork`, `PasswordHasher` and
  `TokenIssuer` in `application/port/`. They are implemented in `infrastructure/`.
- **Response model.** An interactor returns an Output DTO (a plain `record`, the "response model")
  instead of calling a presenter/output boundary. This is a deliberate, documented simplification.
  The Output DTO is a framework-free data structure that crosses the boundary inward-to-outward,
  which respects the Dependency Rule. A presenter interface would add one class per use case and
  change nothing about dependency direction for a JSON API. See D-002 in
  [`../decisions.md`](../decisions.md). Be ready to explain this in the review.
- **Composition root.** Since nothing in `application` is a bean, wiring happens in one place:
  `infrastructure/config/UseCaseConfig` (`@Configuration`). It declares each interactor, each
  application mapper and each application service as a `@Bean`, passing the port implementations
  into constructors. This is the "Main" component of Clean Architecture. It is the only class that
  knows concrete interactors.
- **Domain Services** are still `new`-ed directly by whoever uses them. They're pure, so they don't
  need to be beans.

### Where a port lives

The test is **whose need the port expresses**:

- The *domain's* own vocabulary → `domain/`. `LocalPokemonRepository` ("a synced Pokémon can be
  found by name or number"), `UserAccountRepository`, and `PokemonSource` ("Pokémon can be browsed
  and looked up at the source") live there.
- An *orchestration* need → `application/port/` or `application/query/`. A transaction boundary
  (`UnitOfWork`), hashing a password (`PasswordHasher`), issuing an access token (`TokenIssuer`), a
  screen-shaped read model.

Both are ports: declared inward, implemented in `infrastructure/`.

---

## Naming

Exact patterns, not suggestions.

### Classes

| Type | Pattern | Example |
|---|---|---|
| Domain entity / aggregate root | `{Name}` | `LocalPokemon`, `UserAccount` |
| Value Object | `{Name}` | `LocalPokemonId`, `PokedexNumber`, `PokemonIdentifier`, `Tag` |
| Repository interface | `{Name}Repository` | `LocalPokemonRepository` |
| Repository adapter | `Jpa{Name}Repository` | `JpaLocalPokemonRepository` |
| Spring Data interface | `{Name}JpaRepository` | `LocalPokemonJpaRepository` |
| JPA entity | `{Name}Entity` | `LocalPokemonEntity` |
| JPA mapper | `{Name}EntityMapper` | `LocalPokemonEntityMapper` |
| External data port | `{Name}Source` | `PokemonSource` |
| External data adapter | `{Provider}{PortName}` | `PokeApiPokemonSource` |
| External HTTP client (cached, one method per remote resource) | `{Provider}Client` | `PokeApiClient` |
| Use case — input port (interface) | `{Verb}{Name}UseCase` | `SyncPokemonUseCase` |
| Use case — implementation | `{Verb}{Name}Interactor` | `SyncPokemonInteractor` |
| Application service | descriptive, no fixed suffix | *(none planned — create one only when 2+ interactors share orchestration)* |
| Input DTO | `{Verb}{Name}Input` | `UpdateLocalPokemonInput` |
| Output DTO | `{Verb}{Name}Output` / `{Name}Output` | `LocalPokemonOutput`, `PokemonDetailOutput` |
| Application mapper | `{Name}Mapper` | `LocalPokemonMapper` |
| Orchestration port | `{Name}` — by capability, no `Port` suffix | `UnitOfWork`, `PasswordHasher` |
| Orchestration port adapter | `{Technology}{PortName}` | `SpringUnitOfWork`, `BCryptPasswordHasher`, `JwtTokenIssuer` |
| Read model / adapter / row | `{Name}Query` / `Jdbc{Name}Query` / `{Name}View` | `PokemonListQuery` |
| Domain Service | `{Name}DomainService` | *(none planned — create one only when a rule spans aggregates)* |
| Security error writer (interfaces) | `ErrorResponse{SpringSecurityInterface}` | `ErrorResponseAuthenticationEntryPoint`, `ErrorResponseAccessDeniedHandler` |
| Controller | `{Name}Controller` | `PokemonController`, `AuthController` |
| HTTP request body | `{Verb}{Name}Request` | `UpdateLocalPokemonRequest` |
| HTTP response body | `{Name}Response` | `PokemonDetailResponse`, `LocalPokemonResponse` |
| REST mapper | `{Name}RestMapper` | `PokemonRestMapper` |
| Config class | `{Name}Config` | `SecurityConfig`, `UseCaseConfig` (composition root) |
| Config properties | `{Name}Properties` (record) | `PokeApiProperties`, `JwtProperties` |
| Exception | `{Name}Exception` | `PokemonNotFoundException` |

### Methods

| Situation | Pattern | Example |
|---|---|---|
| Use case entry point (read-only) — declared on the input port | `execute(Input)` | `execute(GetPokemonInput input)` |
| Use case entry point (mutating) | `execute(Input, …, Instant now)` | `execute(input, localPokemonId, now)` |
| Domain factory (new) | `create(...)` | `LocalPokemon.create(id, pokedexNumber, snapshot, now)` |
| Domain factory (from DB) | `builder()` | `LocalPokemon.builder()...build()` |
| Domain state change | action verb | `updateCustomAttributes(...)` |
| Boolean domain check | `is{State}` / `has{Thing}` | `hasTag(tag)` |
| Repository fetch one | `findBy{Field}` | `findByPokedexNumber`, `findByIdentifier` |
| Repository fetch one or throw | `get{Field}` (default method) | `getByIdentifier` |
| Repository fetch many | `findAllBy{Field}` | `findAllByPokedexNumbers(numbers)` |
| Repository persist / remove | `save` / `delete` | `save(pokemon)`, `delete(pokemon)` |
| Transaction boundary | `inTransaction(Supplier)` / `inTransaction(Runnable)` | always with a **block** lambda |

### Tests

| Type | Suffix | Method pattern |
|---|---|---|
| Unit test | `Test` | `should{Result}When{Condition}` |
| Integration test | `IT` | `should{Result}When{Condition}` |

---

## Layer rules

### domain/

- **Zero framework imports.** Only `java..` and `domain` itself — enforced by an ArchUnit
  allowlist. No Spring, no Jakarta, no Jackson, no Lombok, no slf4j.
- **Zero ambient state.** Factories and mutators take `Instant now` as their last parameter. A new
  identity arrives the same way: `{Name}Id.generate()` exists (the one legitimate home of
  `UUID.randomUUID()`), but it is *called* only from `interfaces/`.
- Entities: state changes only through explicit domain methods. **No setters.** Private constructor
  taking the `Builder`; `create(...)` for new instances (owns creation-time invariants),
  `builder()` for reconstitution from persistence.
- **`builder()` is the reconstitution contract, not a construction API.** It null-checks but does
  not re-run creation invariants. Only `{Name}EntityMapper` calls it. Say so in the entity Javadoc.
- Value Objects: always `record`, validated in the compact constructor. No null, no invalid state.
  Limits are public constants on the VO (`FullName.MAX_LENGTH`), and the exception builds its
  message from them (`new InvalidFullNameException(MIN_LENGTH, MAX_LENGTH)`), so the message
  can't drift from the rule. Tests still assert the literal values, because they're the spec.
  Messages reach the user through `ErrorResponse`: start with a capital letter, name the field as
  the user knows it ("Name", "Password"), and state the real rule (the password limit is in bytes).
  A VO holding a secret (`RawPassword`) overrides `toString()` to redact it.
- Repository interfaces declare only what the domain needs, with the domain's own
  `Page`/`PageRequest` (`domain/pagination/`) — never Spring Data types. Pair `findById` with a
  `default getById(id)` that throws the `{Name}NotFoundException`.
- **One repository per aggregate root.** Child values (the tags) are loaded and
  saved through their root.
- **Aggregates are consistency boundaries.** One aggregate per transaction; reference other
  aggregates by id, never by object graph. Prefer small.
- Domain Services (`{Name}DomainService`) are plain objects, `new`-ed where needed — never
  `@Component`, never injected.

#### Exceptions

- `DomainException extends RuntimeException`, abstract. One concrete class per error case.
- **Never store a transport concept** (HTTP status, error code enum) on a domain exception. The
  **message** is the contract: it is what the API returns and the frontend shows. Write it for that
  reader. Never put secrets or raw user input that could be sensitive in it.
- Shared meaning + shared HTTP mapping → extend an abstract **category**. This project's
  categories and their mapping (in `GlobalExceptionHandler`, keyed on the category only):

| Category (abstract) | HTTP | `code` | Typical members |
|---|---|---|---|
| `NotFoundException` | 404 | `NOT_FOUND` | `PokemonNotFoundException` (not in PokeAPI), `LocalPokemonNotFoundException` (not synced) |
| `ConflictException` | 409 | `CONFLICT` | `PokemonAlreadySyncedException`, `EmailAlreadyRegisteredException`, `LocalPokemonModifiedConcurrentlyException` |
| `ValidationException` | 400 | `VALIDATION_ERROR` | `InvalidTagException`, `InvalidEmailException`, `WeakPasswordException`, `InvalidPageRequestException` |
| `UnauthenticatedException` | 401 | `UNAUTHENTICATED` | `InvalidCredentialsException`, `UnknownAccountException` |
| `DomainException` (catch-all) | 422 | `DOMAIN_ERROR` | one-off business rule violations with no sibling |

- **PokeAPI being down is not a domain exception.** The `PokemonSource` port declares
  `PokemonSourceUnavailableException extends RuntimeException` in `domain/source/` as part of its
  contract; the adapter throws it for timeouts/5xx/IO errors; `GlobalExceptionHandler` maps it to
  `503 SOURCE_UNAVAILABLE`. A PokeAPI 404 is a business fact and becomes
  `PokemonNotFoundException` (404).

### application/

- **No framework, at all.** No annotations from Spring/Jakarta/Jackson, no Spring types in
  signatures, no logging framework. If an interactor genuinely needs to log, that's a signal the
  concern belongs in an adapter, or behind a port.
- Use cases: an input-port interface `{Verb}{Name}UseCase` (one `execute` method) plus one plain
  `{Verb}{Name}Interactor` implementing it. Constructor takes ports and pure collaborators, and the
  wiring happens in `UseCaseConfig`. A use case is one atomic operation. It orchestrates, it does
  not decide.
- **Never read the clock, never generate an identity.** `now` is the last parameter of `execute`; a
  new aggregate's id is an explicit parameter too. Both come from `interfaces/`.
- **Input DTO → domain construction lives in a `{Name}Mapper`** (plain class,
  `application/mapper/`, wired in `UseCaseConfig` or constructed by the interactor's caller), never inline in the use case — also when the input arrives as raw
  parameters rather than a record. The mapper takes `id`/`now` as parameters and is a pure function
  (no ports). Split a mapper per use case once it passes ~4 methods.
- **A use case never pre-validates and silently skips.** Invariants are owned by the domain
  (factory / VO constructor / behaviour method), which throws. No `if (...) { log; return; }`.
- Use cases call `repository.getById(...)` for find-or-throw; business logic stays in domain
  methods called on the retrieved object.
- Input and Output DTOs: always `record`. Output DTOs have a static `from(DomainObject)`.
- No Spring Data types, no JPA entities, no Jackson annotations here.

#### The transaction boundary

- Explicit, through the `UnitOfWork` port — which is also what keeps `@Transactional` out of a
  framework-free layer (`application/port/`) — `inTransaction(Supplier<T>)` plus
  a default `inTransaction(Runnable)`. **Always call it with a block lambda** (`() -> { ... }`); a
  bare-expression lambda is an ambiguous overload.
- `@Transactional` appears in exactly one class: `SpringUnitOfWork` (`infrastructure/transaction/`,
  `TransactionTemplate`-based). Never on use cases, never on repository adapters.
- **Never call PokeAPI inside `inTransaction`.** Fetch the PokeAPI data first, then open the
  transaction for the read-check-write against the database. A remote call holding a DB
  connection/transaction open is a classic pool-exhaustion bug — and it's a good thing to point out
  in the review.
- A transaction is **not** mutual exclusion: uniqueness (`pokedex_number`, `email`) is a unique
  constraint in the migration, and the adapter translates `DataIntegrityViolationException` on it
  into the matching `ConflictException`.
- A transaction is **not** a lost-update guard: every `{Name}Entity` has `@Version`; the adapter
  translates `OptimisticLockingFailureException` into a `*ModifiedConcurrentlyException`
  (`ConflictException`, 409).
- A non-critical action after commit (none planned yet) is wrapped in `try`/`catch` + `log.warn`, so
  it can never turn a committed write into a failed response.

#### The read path

- Repository ports return whole aggregates — right for writes and for a single-aggregate paginated
  list. A **read model** (`application/query/{Name}Query` → `{Name}View` records, implemented in
  `infrastructure/persistence/query/`) is only for screens that span more than one aggregate. It is
  read-only by construction. Don't create one until a screen actually needs it.
- The merged views (PokeAPI + local record) are **not** read models: they combine the
  `PokemonSource` port with one aggregate, which the use case does directly (port + repository).
  Local data for a list page comes in one query (`findAllByPokedexNumbers`), never one per item.

### infrastructure/

- JPA entities only in `infrastructure/persistence/entity/`. Table name = entity name in
  `snake_case` plural (`LocalPokemonEntity` → `local_pokemons`; `UserAccountEntity`
  → `user_accounts`).
- `Jpa{Name}Repository` implements the domain port, maps with `{Name}EntityMapper`
  (`@Component`: `toDomain` rebuilds via `builder()`; `toEntity` for a new row; `copyInto` onto the
  **managed** entity for an update, so `@Version` sees the version that was read (D-011)), and
  translates pagination and technical exceptions. **Maps the whole aggregate, children included** — the IT asserts it.
- **Every `{Name}Entity` carries `@Version`.**
- **No `@ManyToOne` / `@OneToOne` / `@ManyToMany`, ever.** Cross-aggregate references are plain id
  columns with the FK declared in the migration. Child values inside one aggregate use
  `@ElementCollection` + `@CollectionTable` (or a unidirectional `@OneToMany` + `@JoinColumn` on
  the root if the child has identity).
- JPA no-arg constructor is `public` (the mapper lives in a sibling package).
- **Schema is owned by Flyway.** `spring.jpa.hibernate.ddl-auto: validate`. Migrations in
  `src/main/resources/db/migration/V{n}__{description}.sql`, plain SQL, never edited once
  committed — a fix is a new migration. Seed/demo data is a migration too
  (`V{n}__seed_demo_data.sql`), with fixed UUIDs and a BCrypt hash, never a plaintext password.
- **Boot 4 dependency gotchas:** `spring-boot-starter-flyway` (not bare `flyway-core`) plus
  `flyway-database-postgresql`; `spring-boot-starter-data-jpa` is **not** in the scaffold yet and must
  be added; Boot 4 ships **Jackson 3** (`tools.jackson.*` packages, `JsonMapper`) — don't import
  `com.fasterxml.jackson.databind` by habit. Verify any coordinate you're unsure of against the
  resolved dependency tree (`./gradlew dependencies`) instead of guessing.

#### PokeAPI adapter (`infrastructure/external/pokeapi/`)

- Two beans, each with one job:
  - `PokeApiClient` (`@Component`) does the HTTP: one public method per PokeAPI resource (list
    page, pokemon, species, evolution chain), built on Spring `RestClient` with explicit
    connect/read timeouts and base URL from
    `PokeApiProperties` (`@ConfigurationProperties("pokeapi")`). It returns the package-private
    JSON records. **It is the only place `@Cacheable` appears.**
  - `PokeApiPokemonSource implements PokemonSource` (`@Component`) composes those calls, runs
    the fan-out, and translates JSON → domain through `PokeApiTranslator`. It has no `@Cacheable`.
- **Boot 4 dependency:** the `RestClient` *class* is in `spring-web`, but the auto-configured
  `RestClient.Builder` bean and `@RestClientTest` live in `spring-boot-starter-restclient` and
  `spring-boot-starter-restclient-test` (verified on Maven Central for 4.1.1). Add both in plan
  task S2.3.
- PokeAPI JSON is deserialized into package-private records local to the adapter
  (`PokeApiPokemonJson`, `PokeApiSpeciesJson`, `PokeApiEvolutionChainJson`, …) annotated with
  `@JsonIgnoreProperties(ignoreUnknown = true)`. **No PokeAPI type leaves the package** — the
  adapter translates to `domain/source` records.
- Translation rules (encode them in adapter tests):
  - `weight` is **hectograms** → kg = `weight / 10`; `height` is **decimetres** → m = `height / 10`.
  - Sprite: `sprites.front_default`; artwork: `sprites.other["official-artwork"].front_default`
    (either may be `null` → handle explicitly).
  - Category = English entry of `pokemon-species.genera[].genus` (e.g. "Seed Pokémon").
  - Description = the English `flavor_text_entries[]` entry with the highest version id (from
    `version.url`), normalized (`\n`, `\f`, soft hyphen →
    single spaces).
  - Evolution lineage = recursive walk of `evolution-chain.chain.evolves_to[]` (it branches — Eevee).
- Errors: PokeAPI 404 → empty, which the port turns into `PokemonNotFoundException`; timeout / 5xx /
  I/O → `PokemonSourceUnavailableException`. Never let a `RestClientException` escape the adapter.
- Browsing a page needs N `/pokemon/{id}` + N `/pokemon-species/{id}` calls (the list endpoint only
  returns names). Fetch them **concurrently** (virtual threads — `spring.threads.virtual.enabled:
  true` / an executor of virtual threads) and **cache per Pokémon**, so a page is fast after the
  first hit. Cap page size (≤ 50).
- **Caching** (the brief's nice-to-have): Spring Cache abstraction with `@Cacheable` on
  `PokeApiClient`'s public methods only (never in `domain`/`application`, never on
  `PokeApiPokemonSource`), backed by Caffeine with a TTL and a max size configured in
  `application.yaml`. One cache per remote resource: `pokeapi-pages`, `pokeapi-pokemon`,
  `pokeapi-species`, `pokeapi-evolution-chains`. Errors are not cached. Local data is never cached.
- **Why a separate client bean (the self-invocation trap).** Spring caching works through a proxy,
  so only calls that *enter the bean from outside* are cached. Interactors call
  `PokemonSource.getByIdentifier`, a `default` method that calls `this.findByIdentifier` inside
  the target object, which bypasses the proxy. `@Cacheable` on `findByIdentifier` would therefore
  never hit for detail or sync, and per-entry caching inside `findAll` would fail the same way.
  Calls from the source adapter into `PokeApiClient` always cross a bean boundary, so they are
  cached. The caching test goes through `getByIdentifier` to prove it.
- Never log full response bodies. Log PokeAPI call failures with the URL path and status only.

#### Security (`infrastructure/security/`, `infrastructure/config/SecurityConfig`)

- Stateless JWT. Spring Security's own JOSE support (`spring-boot-starter-oauth2-resource-server`,
  HS256 with a secret from `JwtProperties`, `NimbusJwtEncoder`/`NimbusJwtDecoder`) — no third-party
  JWT library.
- `BCryptPasswordHasher implements PasswordHasher`; `JwtTokenIssuer implements TokenIssuer`. The
  use cases only see the ports.
- Route policy (D-030) is declared **once**, in `SecurityConfig`, and mirrored in
  [`../domain-model.md`](../domain-model.md#api-contract): **every read is public** (list, detail,
  `GET …/local`), as are register, login and `/actuator/health`. **Only the writes** (`POST`, `PUT`,
  `DELETE` on `/api/v1/pokemon/*/local`) and `/auth/me` require a valid token. Other actuator
  endpoints are not exposed.
- CSRF disabled (stateless bearer tokens, no cookies). **No CORS configuration**: the browser
  only ever talks to one origin. In Docker, nginx serves the SPA and proxies `/api` to the
  backend. In development, the Vite dev server proxies `/api` (D-019).
- 401/403 from the security layer return the same `ErrorResponse` JSON as everything else. The
  writers live in **`interfaces/rest/security/`** (`ErrorResponseAuthenticationEntryPoint`,
  `ErrorResponseAccessDeniedHandler`), next to `ErrorResponse`, because writing the HTTP error
  shape is a delivery concern. `SecurityConfig` receives them as Spring Security's own
  `AuthenticationEntryPoint` / `AccessDeniedHandler` interfaces, so `infrastructure` never imports
  `interfaces` (ArchUnit). Register the entry point both in `exceptionHandling(...)` and in
  `oauth2ResourceServer(...)`, otherwise an invalid/expired bearer token gets the resource
  server's default response instead of `ErrorResponse` (verify the configurer method names
  against the resolved Spring Security version).
- The JWT secret has a dev default in `application.yaml` only for local/demo use and is overridden
  by an env var in Docker; the README says so.

### interfaces/

- Controllers are thin: `@Valid` on the request, call **one** use case through its input-port
  interface, map, return. No try/catch, no business logic. Never reference an `*Interactor`.
- **The edge.** Only `interfaces/` reads the clock (`Instant.now(clock)` via the injected `Clock`
  bean, once per request) and mints new ids (`LocalPokemonId.generate()`, `UserId.generate()`). Both are
  passed into the use case.
- `{Name}RestMapper` (`@Component`) converts request ↔ Input and Output → response. Controllers never
  return domain objects (enforced by review; the ArchUnit rule for it is parked).
- One `@RestControllerAdvice GlobalExceptionHandler`. Handlers keyed **only** on categories +
  `DomainException` catch-all + framework exceptions. Never a handler for a concrete domain class.
  Must also cover, all as `400 VALIDATION_ERROR` with a consistent body:
  `MethodArgumentNotValidException` (with field errors), `HttpMessageNotReadableException`
  (malformed JSON — the brief's "malformed payloads"), `MethodArgumentTypeMismatchException`,
  `HandlerMethodValidationException`/`ConstraintViolationException`. Plus `PokemonSourceUnavailableException`
  → 503, `NoResourceFoundException` → 404, and a last-resort `Exception` → 500 that logs the stack
  trace and returns a generic message (never the exception text).
- The catch-all `Exception` handler must keep the status of Spring's own web exceptions (anything
  implementing `org.springframework.web.ErrorResponse`: 405, 415, …). Otherwise it turns them into
  500s. Only a truly unknown failure is a 500.
- **The API speaks English:** `spring.web.locale: en` with `spring.web.locale-resolver: fixed`.
  Otherwise Bean Validation messages follow the browser's `Accept-Language` (a pt-BR browser gets
  "não deve estar em branco"). (`spring.mvc.locale` is the deprecated name.)
- **One error shape everywhere:**
  `ErrorResponse(String code, String message, List<FieldError> fieldErrors)` where `fieldErrors`
  is empty when not applicable.
- **One page shape everywhere:** `PageResponse<T>(List<T> content, int page, int size, long
  totalElements, int totalPages)`.
- Plural resource nouns. Standard verbs and statuses: `GET` 200, `POST` create 201 (+ `Location`),
  `PUT` 200, `DELETE` 204.

#### Base path

- Every controller maps under the plain prefix **`/api/v1`** (D-029), so the published contract is
  labelled from day one. Native Spring API versioning is parked. A breaking change would be a new
  `/api/v2` controller calling the same input ports, so versioning stays an `interfaces/` concern.
- Security matchers use the same prefix (`/api/v1/pokemon/**`).

- OpenAPI docs via springdoc (`/swagger-ui.html`), **timeboxed to 30 minutes** (D-016). If no
  release works with Boot 4.1 on the first try, drop it. The README's
  `curl` examples are the fallback.

---

## Testing

### What to use per layer

| Layer | Annotation | Dependencies |
|---|---|---|
| domain | none (plain JUnit 5) | none |
| application (`{Verb}{Name}InteractorTest`) | `@ExtendWith(MockitoExtension.class)` | mock **ports**, real pure collaborators (mappers) |
| infrastructure (persistence) | `@DataJpaTest` + Testcontainers PostgreSQL | real DB |
| infrastructure (PokeAPI adapter) | `@RestClientTest` + `MockRestServiceServer`, JSON fixtures in `src/test/resources/pokeapi/` | no network |
| interfaces (REST) | `@WebMvcTest` + `@MockitoBean` on the **input-port interfaces** + `@Import` RestMapper/fixed `Clock`/security config | mock use cases |
| composition root | `@SpringBootTest` context-loads IT | every `*UseCase` interface resolves to a bean |
| end-to-end (few) | `@SpringBootTest` + Testcontainers | real app + real DB, PokeAPI stubbed |
| architecture | ArchUnit `LayeredArchitectureTest` | none |

### TDD, inward-out — one full cycle per layer

1. **Domain** — plain JUnit, no mocks. Most of the thinking happens here.
2. **Use case** — ports mocked, domain real, mapper real.
3. **Adapters** — repository against real Postgres, controller against the real HTTP stack, PokeAPI
   adapter against recorded JSON.

Red → confirm it fails **for the right reason** → Green (minimum code) → Refactor → next.

### Rules

- Domain and application tests have zero Spring context.
- AssertJ only (`assertThat`, `assertThatThrownBy`). BDD Mockito only (`given`/`then`/`willReturn`).
- **Don't mock pure collaborators** (mappers, domain services). Mock what crosses a boundary.
- Assert outcomes; verify an interaction only when its effect isn't observable otherwise.
- Stub a repository's `getById` directly — Mockito does not run a mocked interface's default method.
- One **fixture (Object Mother)** per aggregate in the test source set (`PokemonFixture`,
  `UserAccountFixture`) with a fixed `NOW`. Fixtures never read the clock.
- Slices don't scan plain `@Component`s: `@DataJpaTest` → `new {Name}EntityMapper()`;
  `@WebMvcTest` → `@Import({Name}RestMapper.class)`. With Spring Security on the classpath,
  `@WebMvcTest` also needs the security config **and** the `interfaces/rest/security` error
  writers imported (they're plain `@Component`s), plus `spring-security-test` helpers like
  `with(jwt())` — test both the 401 and the authorized path for protected routes.
- Use `Optional` assertions (`hasValueSatisfying`), never `.get()`, also in tests.
- Each REST endpoint has tests for: happy path, 400 (invalid body **and** malformed JSON), 404,
  401 where protected, 409 where applicable.
- Testcontainers coordinates change between major versions (2.x moved to
  `org.testcontainers:testcontainers-postgresql`, `org.testcontainers.postgresql.PostgreSQLContainer`).
  Verify against the resolved dependency, not memory. Prefer Spring Boot's `@ServiceConnection` over
  hand-written `@DynamicPropertySource`.

### Architecture enforcement (ArchUnit, `LayeredArchitectureTest`, runs in the unit `test` task)

`static final ArchRule` fields in `snake_case`. Full code: [`../examples/tests.md#architecture-test`](../examples/tests.md#architecture-test). At minimum:

- `domain` depends only on `java..` and `domain` (allowlist, not denylist).
- `application` depends only on `java..`, `domain` and `application`. Like the domain rule, this is
  an **allowlist**, and it's what makes "no `@Service` in a use case" mechanical instead of a
  convention.
- `interfaces` doesn't depend on `infrastructure`, and doesn't depend on any `*Interactor` class
  (only on input-port interfaces).
- `infrastructure` doesn't depend on `interfaces` (the two adapter rings know nothing about each
  other; Spring wires them through framework interfaces and application ports).
- Every `*Interactor` implements exactly one interface from `..application.usecase..`. Every
  interface in `..application.usecase..` ends with `UseCase`.
- `@Entity` only in `..infrastructure.persistence.entity..`.
- No `@ManyToOne`/`@OneToOne`/`@ManyToMany`.
- No `Optional.get()`. No `@Autowired` fields. No `set*` methods in `domain`.
- No no-arg `Instant.now()`/`LocalDate.now()` outside `interfaces`.
- `UUID.randomUUID()` only inside `..domain.model..`.

All of these use ArchUnit's fluent API. Three stronger rules that need a custom `ArchCondition` are
parked (`plan.md`): a domain model type in a generic return signature, `@Version` on every
`@Entity`, and `generate()` called only from `interfaces`. They're still **rules**, enforced by
review and the ITs, just not mechanically.
- `@Transactional` only in `..infrastructure.transaction..`.
- Non-record classes in `..domain.model..` have only private constructors.

### Build: make sure the tests actually run

Gradle's `test` task ignores `*IT`. `build.gradle` gives integration tests their own task, wired
into `check`. It was proven once by making an IT fail on purpose. On Gradle 9, a custom `Test` task
must set `testClassesDirs` and `classpath` itself:

```groovy
def integrationTest = tasks.register('integrationTest', Test) {
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching '*IT' }
    shouldRunAfter tasks.named('test')
}
tasks.named('test') { filter { excludeTestsMatching '*IT' } }
tasks.named('check') { dependsOn integrationTest }
```

Gradle 9 also **fails a test task that finds no tests** (`failOnNoMatchingTests` on the filter,
`failOnNoDiscoveredTests` on the task). That's a useful guard against a broken filter. It's
switched off on `test` only while no unit test exists, and switched back on in plan task 1.1.

Coverage: JaCoCo report — evidence for the brief's "thorough unit test coverage".
Controller and repository tests are `*IT`, so the report reads the execution data of **both**
`test` and `integrationTest`, or the adapters would show near 0% (see `build.gradle`). Report:
`build/reports/jacoco/test/html/index.html` after `./gradlew check`.

Treat the number as a signal, not a goal.

---

## Spring Boot rules

- Constructor injection always. No `@Autowired` fields. **No Lombok**: records, explicit
  builders and constructors (D-014; removed from the build).
- `application.yaml` only, single profile until a real need appears. Env-specific values come from
  environment variables (`${DB_URL:jdbc:postgresql://localhost:5433/pokedex}`; the local Postgres
  from `docker-compose.yml` is published on host port 5433).
- `@ConfigurationProperties` records for grouped config (`pokeapi.*`, `security.jwt.*`). Add a
  group to `application.yaml` together with the class that binds it, never ahead of it. No raw
  `@Value` groups.
- Logging: `LoggerFactory.getLogger(getClass())`. No `System.out`. **Never log passwords, tokens,
  JWTs, request bodies, or other user-supplied free text** — log ids and metadata.
- `Optional.get()` forbidden — `orElseThrow`, `orElse`, `ifPresent`.
- A `Clock` bean (`Clock.systemUTC()`) in `infrastructure/config/ClockConfig`.

---

## Prohibitions

| Forbidden | Reason |
|---|---|
| `@Entity` outside `infrastructure/persistence/entity/` | Leaks persistence into domain |
| Any non-`java.*` import in `domain/` or `application/` (incl. `@Service`/`@Component`/`@Transactional`) | Use cases and entities must be independent of frameworks — strict Clean Architecture |
| A controller depending on an `*Interactor` instead of its `*UseCase` interface | Breaks the input-port boundary |
| Setters / public constructors on domain entities | Bypasses invariants and `create()`/`builder()` |
| Business logic in use cases or controllers | Use cases orchestrate, controllers deliver; the domain decides |
| Domain objects returned from controllers | Breaks encapsulation; use Output DTO → Response |
| JPA entities or PokeAPI JSON types outside their package | Leaks infrastructure upward |
| `@Autowired` on fields / Lombok | Hidden dependencies / generated code reviewers can't read |
| `Optional.get()` | Silent unwrap |
| `Instant.now()` / `{Id}.generate()` outside `interfaces/` | Hidden ambient state; untestable |
| DTO → domain construction inline in a use case | The mapper is the single source of that translation |
| `@Transactional` outside `infrastructure/transaction/` | A boundary nobody can locate |
| A use case writing outside a `UnitOfWork` | "Atomic" with nothing making it atomic |
| A remote (PokeAPI) call inside `inTransaction` | Holds a DB transaction open on network latency |
| `{Name}Entity` without `@Version` | Silent lost update |
| `@ManyToOne` / `@OneToOne` / `@ManyToMany` | Couples aggregates into one object graph |
| `ddl-auto` other than `validate` | Schema belongs to Flyway |
| Editing a committed migration | Breaks every DB that already ran it |
| A `GlobalExceptionHandler` method keyed on a concrete domain exception | Categories carry the mapping |
| Mocking a mapper or other pure collaborator | Encodes call structure into the test |
| Early-return guard instead of letting the domain throw | Silent no-op indistinguishable from success |
| Logging secrets or user free text | Exposure through logs regardless of level |
| A new dependency without explicit approval | Uncontrolled scope creep |

---

## Reference

- [`../examples/domain.md`](../examples/domain.md) — Entity, Value Object, Pagination, Repository, Exceptions
- [`../examples/application.md`](../examples/application.md) — Input port + Interactor (sync, update, delete, browse, authenticate), `UnitOfWork`, Mapper, DTOs, composition root, when to add a read model
- [`../examples/infrastructure.md`](../examples/infrastructure.md) — Migrations, JPA Entity, Spring Data, Repository Adapter, Entity Mapper, `UnitOfWork` adapter, PokeAPI client + adapter + cache, security adapters
- [`../examples/rest.md`](../examples/rest.md) — Base path, Controller, request bodies, REST Mapper, response shapes, GlobalExceptionHandler, security error writers
- [`../examples/tests.md`](../examples/tests.md) — Fixtures, unit tests, integration tests, Architecture test

The examples are written against the current model. If an example and this file ever disagree,
this file wins (precedence in [`AGENTS.md`](../../AGENTS.md) §3), and the example is fixed in the
same change.
