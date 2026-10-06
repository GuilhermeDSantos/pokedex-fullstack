# Domain Model & API Contract

The project's real model. The code examples in [`examples/`](examples/) are written against exactly
these types. If the model changes, update this file **and** the examples in the same commit, and
log the reason in [`decisions.md`](decisions.md).

Requirement IDs (`US-01`, `TR-DB-1`, …) refer to [`requirements.md`](requirements.md).

---

## Product vision

**A Pokémon catalog service backed by PokeAPI, with a local database for the organization's own
data.**

- **PokeAPI is the source** of canonical Pokémon data (name, sprite, category, weight, types,
  abilities, stats, description, evolution chain). The backend is the only thing that talks to it.
  The frontend never knows where a field came from.
- **Anyone can browse and view Pokémon**, logged in or not (US-01, US-02).
- **A logged-in user can sync a Pokémon into the local database** (US-03). The backend copies a
  snapshot of the PokeAPI data into PostgreSQL. Nobody types Pokémon data by hand. You choose
  *which* Pokémon, and the data comes from PokeAPI.
- **Once synced, a Pokémon carries proprietary fields** that PokeAPI doesn't have: a localized name,
  a region, and internal classification tags. These are the brief's own three examples. A logged-in
  user can edit them or remove the local record (US-04).
- **Local data is shared.** There's one local record per Pokémon, maintained by the team. It isn't
  a personal collection. Users exist to separate public reads from authenticated writes
  (TR-AUTH-3).
- **One resource, merged by the backend.** `GET /pokemon` and `GET /pokemon/{id}` return PokeAPI data
  merged with the local record when one exists. The localized name, if set, becomes `displayName`,
  and the original name is still returned.

The frontend is two pages, a **list** and a **detail**, plus login and register. Sync, edit and
remove all happen on the detail page.

---

## Ubiquitous language

| Term | Meaning |
|---|---|
| **PokeAPI** / **source** | The external source of canonical data. Read-only from our side. In code: the `PokemonSource` port. |
| **Local record** / **local Pokémon** | A Pokémon synced into our PostgreSQL database. The `LocalPokemon` aggregate. It's shared and has no owner. |
| **Sync** | Creating the local record of a Pokémon from PokeAPI data (US-03). The brief calls it "Data Synchronization". |
| **Snapshot** | The scalar subset of PokeAPI data copied into the local record at sync time (name, category, height, weight, sprite, artwork, description). |
| **Custom attributes** | The proprietary fields only we own (US-03.a, US-04): `localizedName`, `region`, `tags`. |
| **Localized name** | The name the organization displays, e.g. a translation for a local market. Free text, optional (D-027). |
| **Display name** | `localizedName` when it's set, otherwise the original name. Computed by the domain. |
| **Category** | The species *genus* ("Seed Pokémon"). It isn't the type. See D-010. |
| **Skills** | The Pokémon's **abilities** (US-01: "a collection of their skills"). See D-010. |
| **Mass** | Weight in kilograms (PokeAPI sends hectograms). |
| **Evolutionary lineage** | The species tree from PokeAPI's evolution chain. It can branch (Eevee). |
| **User account** | A registered user who can authenticate, and so sync, edit and remove local records. |

---

## Aggregates

Two aggregates, plus one outbound port to PokeAPI. They have no reference to each other: local
records are shared, not owned.

```
┌──────────── LocalPokemon (aggregate root) ─────────────┐   ┌──── UserAccount (aggregate root) ────┐
│ LocalPokemonId id            (UUID, minted at the edge) │   │ UserId id                             │
│ PokedexNumber pokedexNumber  (unique)                   │   │ Email email            (unique)       │
│ PokemonSnapshot snapshot     (copied from PokeAPI)      │   │ FullName name                         │
│   └ name (unique), category, height, weight,            │   │ PasswordHash passwordHash             │
│     spriteUrl, artworkUrl, description                  │   │ Instant createdAt                     │
│ CustomAttributes custom      (ours — US-03.a / US-04)   │   └───────────────────────────────────────┘
│   ├ localizedName (optional)                            │
│   ├ region        (optional)                            │
│   └ Set<Tag> tags (0..10)                               │
│ Instant syncedAt, updatedAt                             │
└─────────────────────────────────────────────────────────┘
```

The local record stores a **scalar snapshot** and not the full profile (D-031). Types, abilities,
stats and the evolution chain are always read from PokeAPI, because every screen that shows them
fetches PokeAPI anyway. The snapshot still satisfies US-03 ("persist Pokémon data") and keeps
persistence to two tables. Tags are child values of the aggregate (`@ElementCollection`), with no
repository of their own.

### Value Objects (`domain/model`, all `record`, validated in the compact constructor)

| VO | Rule (violation → exception, category) |
|---|---|
| `LocalPokemonId(UUID value)` | non-null; `generate()` (edge only) |
| `UserId(UUID value)` | same shape |
| `PokedexNumber(int value)` | `value ≥ 1` → `InvalidPokedexNumberException` (Validation) |
| `PokemonIdentifier(String value)` | trimmed, lower-cased; digits or `[a-z0-9-]+`, ≤ `MAX_LENGTH` (100) → `InvalidPokemonIdentifierException` (Validation). A name **or** a number: `isNumber()`, `asNumber()` |
| `Height(BigDecimal meters)` / `Weight(BigDecimal kilograms)` | `≥ 0`, scale 1. Factories `fromDecimetres(int)` / `fromHectograms(int)` hold the unit conversion |
| `PokemonType(String name)` | non-blank, lower-case |
| `Ability(String name, boolean hidden)` | non-blank name |
| `BaseStat(StatName name, int value)` | `1 ≤ value ≤ 255`. `StatName` enum: `HP, ATTACK, DEFENSE, SPECIAL_ATTACK, SPECIAL_DEFENSE, SPEED` |
| `PokemonProfile(...)` | The full PokeAPI view: name, category, height, weight, spriteUrl, artworkUrl, `types` 1..2, `abilities` ≥ 1, `stats` one per `StatName` in `StatName` order, description. Urls, description and category are nullable (PokeAPI has gaps). `toSnapshot()` returns the scalar subset |
| `PokemonSnapshot(...)` | name non-blank; category, height, weight, spriteUrl, artworkUrl, description; urls, category and description nullable |
| `Tag(String value)` | trimmed, lower-cased, `^[a-z0-9][a-z0-9-]{0,29}$` → `InvalidTagException` (Validation) |
| `CustomAttributes(String localizedName, String region, Set<Tag> tags)` | strings trimmed, blank → `null`, ≤ `MAX_TEXT_LENGTH` (100) chars each; ≤ `MAX_TAGS` (10) tags → `InvalidCustomAttributesException` (Validation). `CustomAttributes.empty()` |
| `Email(String value)` | null rejected; trimmed, lower-cased, basic format, ≤ `MAX_LENGTH` (254) → `InvalidEmailException` (Validation) |
| `FullName(String value)` | the user's name, free text as they write it (one field, no first/last split); null rejected; trimmed, `MIN_LENGTH`..`MAX_LENGTH` (2..100) chars → `InvalidFullNameException` (Validation) |
| `RawPassword(String value)` | ≥ 8 chars and ≤ 72 **bytes** in UTF-8 (BCrypt's limit is in bytes: Spring Security 7.1.1 `BCrypt.hashpw` throws above it), at least one letter and one digit → `WeakPasswordException` (Validation). **`toString()` is redacted.** Never stored and never logged |
| `PasswordHash(String value)` | non-blank. Opaque to the domain |

### `LocalPokemon` behaviour

| Method | Rule |
|---|---|
| `static create(LocalPokemonId, PokedexNumber, PokemonSnapshot, Instant now)` | Custom attributes start empty, `syncedAt = updatedAt = now` |
| `updateCustomAttributes(CustomAttributes, Instant now)` | Replaces all custom attributes (PUT semantics), `updatedAt = now`. Snapshot untouched |
| `displayName()` | `localizedName` if set, otherwise `snapshot.name()` |
| `static builder()` | **Reconstitution only** (`LocalPokemonEntityMapper`) |

### `UserAccount` behaviour

| Method | Rule |
|---|---|
| `static register(UserId, Email, FullName, PasswordHash, Instant now)` | `createdAt = now`; exposed as `getName()` |
| `static builder()` | Reconstitution only |

Password policy runs on `RawPassword` **before** hashing. The domain never sees a hash being
computed: that happens behind the `PasswordHasher` port.

### PokeAPI port (`domain/source`)

```java
public interface PokemonSource {
    Page<PokemonSummary> findAll(PageRequest pageRequest);                 // US-01
    Optional<PokemonDetail> findByIdentifier(PokemonIdentifier identifier); // US-02, US-03

    default PokemonDetail getByIdentifier(PokemonIdentifier identifier) {
        return findByIdentifier(identifier).orElseThrow(() -> new PokemonNotFoundException(identifier));
    }
}
```

| Type | Shape |
|---|---|
| `PokemonSummary` | `PokedexNumber number, String name, String spriteUrl, String category, Weight weight, List<PokemonType> types, List<Ability> abilities` (US-01: sprite, category, mass, skills) |
| `PokemonDetail` | `PokedexNumber number, PokemonProfile profile, EvolutionStage evolutionChain` (US-02: image, stats, description, lineage) |
| `EvolutionStage` | `String speciesName, PokedexNumber number, List<EvolutionStage> evolvesTo` (recursive tree) |
| `PokemonSourceUnavailableException extends RuntimeException` | Part of the port contract, not a `DomainException`. Maps to 503 |

Every method may throw `PokemonSourceUnavailableException`.

### Repository ports (`domain/repository`)

```java
public interface LocalPokemonRepository {
    LocalPokemon save(LocalPokemon pokemon);
    Optional<LocalPokemon> findByPokedexNumber(PokedexNumber number);
    Optional<LocalPokemon> findByIdentifier(PokemonIdentifier identifier);   // number → pokedex_number, name → name
    default LocalPokemon getByIdentifier(PokemonIdentifier identifier) {
        return findByIdentifier(identifier).orElseThrow(() -> new LocalPokemonNotFoundException(identifier));
    }
    List<LocalPokemon> findAllByPokedexNumbers(Collection<PokedexNumber> numbers);  // the list merge: 1 query per page
    void delete(LocalPokemon pokemon);
}
```

`UserAccountRepository`: `save`, `findById`, `findByEmail`. No `getById`: a missing account is
never a 404. At login it's `InvalidCredentialsException`, and on `/auth/me` it's
`UnknownAccountException` (401), so the frontend clears the session (D-033).

### Exceptions (`domain/exception`)

```
DomainException (abstract)                          → 422 DOMAIN_ERROR (catch-all; no member yet)
├── NotFoundException (abstract)                    → 404 NOT_FOUND
│   ├── PokemonNotFoundException                    (PokeAPI has no such Pokémon)
│   └── LocalPokemonNotFoundException               (exists in PokeAPI, but not synced)
├── ConflictException (abstract)                    → 409 CONFLICT
│   ├── PokemonAlreadySyncedException
│   ├── LocalPokemonModifiedConcurrentlyException
│   └── EmailAlreadyRegisteredException
├── ValidationException (abstract)                  → 400 VALIDATION_ERROR
│   ├── InvalidPokedexNumberException, InvalidPokemonIdentifierException, InvalidPageRequestException
│   ├── InvalidTagException, InvalidCustomAttributesException
│   └── InvalidEmailException, InvalidFullNameException, WeakPasswordException
└── UnauthenticatedException (abstract)             → 401 UNAUTHENTICATED
    ├── InvalidCredentialsException                 (same message for unknown email and wrong password)
    └── UnknownAccountException                     (valid token, but the account no longer exists — D-033)
```

The 422 catch-all stays in the handler as the safety net for a future one-off business rule, so
that one can never fall through to a 500.

---

## Application layer (input ports → interactors)

All of these are framework-free. Each `*UseCase` is an interface, implemented by an `*Interactor`
and wired in `infrastructure/config/UseCaseConfig`.

| Input port | `execute` signature | Story | Notes |
|---|---|---|---|
| `BrowsePokemonUseCase` | `PageOutput<PokemonSummaryOutput> execute(BrowsePokemonInput)` | US-01 | `source.findAll` + **one** `repository.findAllByPokedexNumbers` for the page → merge |
| `GetPokemonUseCase` | `PokemonDetailOutput execute(GetPokemonInput)` | US-02 | `source.getByIdentifier` + `repository.findByPokedexNumber` → merge; `local` is `null` when not synced |
| `SyncPokemonUseCase` | `LocalPokemonOutput execute(SyncPokemonInput, LocalPokemonId, Instant now)` | US-03 | PokeAPI fetch **before** the transaction. Inside: already synced → 409, create, save |
| `GetLocalPokemonUseCase` | `LocalPokemonOutput execute(GetLocalPokemonInput)` | US-03 | The resource `Location` points to. 404 if not synced |
| `UpdateLocalPokemonUseCase` | `LocalPokemonOutput execute(UpdateLocalPokemonInput, Instant now)` | US-04 | 404 not synced / 400 / 409 concurrent |
| `RemoveLocalPokemonUseCase` | `void execute(RemoveLocalPokemonInput)` | CRUD-D | 404 not synced |
| `RegisterUserUseCase` | `UserOutput execute(RegisterUserInput, UserId, Instant now)` | TR-AUTH | 409 on duplicate email |
| `AuthenticateUserUseCase` | `AccessTokenOutput execute(AuthenticateUserInput, Instant now)` | TR-AUTH | 401 `InvalidCredentialsException` |
| `GetCurrentUserUseCase` | `UserOutput execute(GetCurrentUserInput)` | TR-AUTH | Account gone (e.g. the database was reset while a token was still valid) → 401 `UnknownAccountException` |

The local write use cases (get-local, update, remove) resolve the identifier against the **local
database only**. They never call PokeAPI.

Output ports in `application/port`: `UnitOfWork`, `PasswordHasher` (`PasswordHash hash(RawPassword)`,
`boolean matches(RawPassword, PasswordHash)`), `TokenIssuer` (`AccessToken issue(UserAccount,
Instant now)` → `AccessToken(String value, Instant expiresAt)`).

---

## Persistence (PostgreSQL, Flyway-owned)

**Design intent only.** The real schema is the Flyway migrations in
`backend/src/main/resources/db/migration/`. Once a migration exists, it wins, and this table is
updated to match (or trimmed), never the other way around.

| Table | Columns | Notes |
|---|---|---|
| `local_pokemons` | `id uuid pk`, `pokedex_number int not null`, `name not null`, `category`, `height_m numeric(5,1)`, `weight_kg numeric(6,1)`, `sprite_url`, `artwork_url`, `description text`, `localized_name`, `region`, `synced_at timestamptz`, `updated_at timestamptz`, `version bigint not null` | `uk_local_pokemons_pokedex_number` → `PokemonAlreadySyncedException`; `uk_local_pokemons_name` (lookup by name) |
| `local_pokemon_tags` | `local_pokemon_id fk → local_pokemons on delete cascade`, `tag` | PK (`local_pokemon_id`, `tag`) |
| `user_accounts` | `id uuid pk`, `email unique`, `display_name`, `password_hash`, `created_at timestamptz`, `version bigint not null` | `uk_user_accounts_email` → `EmailAlreadyRegisteredException` |

Migrations, in the order the slices create them: `V1__create_user_accounts.sql`,
`V2__create_local_pokemons.sql`, `V3__seed_demo_data.sql`. The seed has a demo user with a BCrypt hash, plus about 10 synced Pokémon
with custom attributes and tags, so the demo starts with merged data. **Pikachu is not in the
seed**, because it's synced live in the demo. The snapshot values come from **real PokeAPI
responses** (recorded with `curl`, like the test fixtures), never typed from memory. The demo
credentials are written in the README and nowhere else.

---

## API contract

Base path **`/api/v1`**, a plain prefix (D-029). JSON only. Errors always use `ErrorResponse`, and
lists always use `PageResponse`. `{identifier}` is a name or a Pokédex number (`pikachu` or `25`).
The frontend doesn't know or care which data comes from PokeAPI and which from the database.

| Method & path | Auth | Success | Errors | Story |
|---|---|---|---|---|
| `GET /pokemon?page=0&size=20` | public | 200 `PageResponse<PokemonSummaryResponse>` | 400 bad page/size, 503 | US-01 |
| `GET /pokemon/{identifier}` | public | 200 `PokemonDetailResponse` | 400, 404, 503 | US-02 |
| `GET /pokemon/{identifier}/local` | public | 200 `LocalPokemonResponse` | 400, 404 (not synced) | US-03 |
| `POST /pokemon/{identifier}/local` (no body) | 🔒 | 201 `LocalPokemonResponse` + `Location` | 400, 401, 404 (not in PokeAPI), 409 (already synced), 503 | US-03 / CRUD-C |
| `PUT /pokemon/{identifier}/local` body `{ "localizedName", "region", "tags": [] }` | 🔒 | 200 `LocalPokemonResponse` | 400 (invalid **or** malformed body), 401, 404 (not synced), 409 (modified concurrently) | US-04 / CRUD-U |
| `DELETE /pokemon/{identifier}/local` | 🔒 | 204 | 400, 401, 404 (not synced) | CRUD-D |
| `POST /auth/register` body `{ "email", "name", "password" }` | public | 201 `UserResponse` | 400, 409 | TR-AUTH |
| `POST /auth/login` body `{ "email", "password" }` | public | 200 `{ "accessToken", "tokenType": "Bearer", "expiresAt" }` | 400, 401 | TR-AUTH |
| `GET /auth/me` | 🔒 | 200 `UserResponse` | 401 (no/invalid token, or the account no longer exists) | TR-AUTH |
| `GET /actuator/health` | public | 200 | | ops |

CRUD on the dataset (TR-API-1): **C**reate = sync, **R**ead = `GET …/local` (and the merged
reads), **U**pdate = `PUT …/local`, **D**elete = `DELETE …/local`. Writes go to the `/local`
sub-resource so every verb means exactly what it says: after a `DELETE`, `GET /pokemon/25` is still
200 (the Pokémon exists in PokeAPI), and `GET /pokemon/25/local` is 404.

Shared shapes:

```json
// ErrorResponse
{ "code": "VALIDATION_ERROR", "message": "Request body is invalid",
  "fieldErrors": [ { "field": "tags", "message": "size must be between 0 and 10" } ] }

// PageResponse<PokemonSummaryResponse>
{ "content": [ { "pokedexNumber": 25, "name": "pikachu", "displayName": "Pikachu BR",
                 "spriteUrl": "…", "category": "Mouse Pokémon", "weightKilograms": 6.0,
                 "types": ["electric"], "abilities": [ { "name": "static", "hidden": false } ],
                 "synced": true } ],
  "page": 0, "size": 20, "totalElements": 1302, "totalPages": 66 }

// PokemonDetailResponse — PokeAPI data merged with the local record
{ "pokedexNumber": 25, "name": "pikachu", "displayName": "Pikachu BR",
  "category": "Mouse Pokémon", "heightMeters": 0.4, "weightKilograms": 6.0,
  "spriteUrl": "…", "artworkUrl": "…", "types": ["electric"],
  "abilities": [ { "name": "static", "hidden": false } ],
  "stats": [ { "name": "HP", "value": 35 }, … ], "description": "…",
  "evolutionChain": { "speciesName": "pichu", "pokedexNumber": 172, "evolvesTo": [ … ] },
  "local": { "localizedName": "Pikachu BR", "region": "Kanto", "tags": ["mascot", "starter"],
             "syncedAt": "…", "updatedAt": "…" } }          // "local": null when not synced

// LocalPokemonResponse — the /local sub-resource
{ "pokedexNumber": 25, "name": "pikachu", "displayName": "Pikachu BR",
  "localizedName": "Pikachu BR", "region": "Kanto", "tags": ["mascot", "starter"],
  "syncedAt": "…", "updatedAt": "…" }
```

**Validation (D-028; both kinds are 400, as US-04 requires).** The domain is the single validation
authority. Every rule (format, length, policy) lives in a VO (`Tag`, `CustomAttributes`,
`PokemonIdentifier`, `Email`, `DisplayName`, `RawPassword`) and throws a `ValidationException`
subtype. At the edge, JSON parsing catches malformed bodies and wrong types, and Bean Validation
checks **shape only**: required fields and collection/string sizes, with every limit referencing
the domain constant (`@Size(max = CustomAttributes.MAX_TAGS)`). No format rule exists only at the
edge, so there's no `@Email`.

---

## PokeAPI mapping (verify against https://pokeapi.co/docs/v2 before coding against it)

| Our field | PokeAPI source |
|---|---|
| list page | `GET /pokemon?limit={size}&offset={page*size}` → `count`, `results[].name/url` (id parsed from url) |
| number, name | `GET /pokemon/{id or name}` → `id`, `name` |
| height / weight | `height` (dm), `weight` (hg) → `Height.fromDecimetres`, `Weight.fromHectograms` |
| sprite / artwork | `sprites.front_default` / `sprites.other."official-artwork".front_default` |
| types | `types[]` sorted by `slot` → `type.name` |
| abilities | `abilities[]` sorted by `slot` → `ability.name`, `is_hidden` |
| stats | `stats[]` → `stat.name` (`hp`, `attack`, `defense`, `special-attack`, `special-defense`, `speed`), `base_stat` |
| category | `GET /pokemon-species/{id}` → `genera[]` where `language.name == "en"` → `genus` |
| description | same species → `flavor_text_entries[]` with `language.name == "en"` and the highest version id (parsed from `version.url`; the array order isn't guaranteed), whitespace-normalized |
| lineage | species → `evolution_chain.url` → `GET /evolution-chain/{id}` → recursive `chain.species` / `evolves_to[]` |

Pokémon with ids above 10000 are alternate forms. Their species is the base species, so always
follow `species.url` and don't assume `species id == pokemon id`. Forms are a known limitation
(parked).
