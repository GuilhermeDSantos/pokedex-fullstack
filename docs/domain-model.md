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
- **A logged-in user can sync a Pokémon into the local database** (US-03). The backend records the
  Pokémon (its Pokédex number) in PostgreSQL. Nobody types Pokémon data by hand. You choose
  *which* Pokémon; its data keeps coming from PokeAPI, and the local record holds what's ours.
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
| **PokeAPI** / **source** | The external source of canonical data. Read-only from our side. In code: the `PokemonRepository` port. |
| **Local record** / **local Pokémon** | A Pokémon synced into our PostgreSQL database. The `LocalPokemon` aggregate. It's shared and has no owner. |
| **Sync** | Creating the local record of a Pokémon from PokeAPI data (US-03). The brief calls it "Data Synchronization". |
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
│ CustomAttributes custom      (ours — US-03.a / US-04)   │   │ FullName name                         │
│   ├ localizedName (optional)                            │   │ PasswordHash passwordHash             │
│   ├ region        (optional)                            │   │ Instant createdAt                     │
│   └ Set<Tag> tags (0..10)                               │   └───────────────────────────────────────┘
│ Instant syncedAt, updatedAt                             │
└─────────────────────────────────────────────────────────┘
```

The local record stores the **Pokédex number and our fields**, nothing copied from PokeAPI
(D-039). PokeAPI stays the source of truth: the name, types, abilities, stats and the evolution
chain are always read from it, so nothing local can go stale, and a renamed Pokémon needs no
migration. Tags are child values of the aggregate (`@ElementCollection`, indexed by `tag`), with no
repository of their own.

### Value Objects (`domain/model`, all `record`, validated in the compact constructor)

| VO | Rule (violation → exception, category) |
|---|---|
| `LocalPokemonId(UUID value)` | non-null; `generate()` (edge only) |
| `UserId(UUID value)` | same shape |
| `PokedexNumber(int value)` | `value ≥ MIN_VALUE` (1) → `InvalidPokedexNumberException` ("Pokédex number must be at least 1", Validation) |
| `PokemonIdentifier(String value)` | trimmed, lower-cased; `[a-z0-9-]{1,100}` → else `InvalidPokemonIdentifierException` ("A Pokémon is identified by its name or its Pokédex number", Validation). A name **or** a number; `isNumber()` / `asNumber()` let the local lookup go straight to the database for a number |
| `Height(BigDecimal meters)` / `Weight(BigDecimal kilograms)` | `≥ 0`, scale 1. Factories `fromDecimetres(int)` / `fromHectograms(int)` hold the unit conversion |
| `PokemonType(String name)` | non-blank (else `IllegalArgumentException`: it comes from PokeAPI, so it's a mapping bug), trimmed, lower-cased |
| `Ability(String name, boolean hidden)` | non-blank name |
| `BaseStat(StatName name, int value)` | `1 ≤ value ≤ 255`. `StatName` enum: `HP, ATTACK, DEFENSE, SPECIAL_ATTACK, SPECIAL_DEFENSE, SPEED` |
| `PokemonProfile(...)` | The full PokeAPI view: name, category, height, weight, spriteUrl, artworkUrl, types, abilities, `stats` exactly one per `StatName`, kept in `StatName` order, description. Name, height and weight required; urls, description and category nullable (PokeAPI has gaps). A broken rule here is a mapping bug (`IllegalArgumentException`) |
| `Tag(String value)` | null rejected; trimmed, lower-cased, `[a-z0-9][a-z0-9-]{0,29}` (`MAX_LENGTH` 30) → `InvalidTagException` ("A tag uses only letters, digits and hyphens, starts with a letter or a digit, and has at most 30 characters", Validation). The message never echoes the tag |
| `CustomAttributes(String localizedName, String region, Set<Tag> tags)` | strings trimmed, blank → `null`, ≤ `MAX_TEXT_LENGTH` (100) chars each; ≤ `MAX_TAGS` (10) tags → `InvalidCustomAttributesException` ("Localized name must be at most 100 characters", "Region …", "A Pokémon has at most 10 tags"; Validation). Tags are kept as an unmodifiable copy; a null set is a caller bug. `CustomAttributes.empty()` |
| `Email(String value)` | null rejected; trimmed, lower-cased, basic format, ≤ `MAX_LENGTH` (254) → `InvalidEmailException` (Validation) |
| `FullName(String value)` | the user's name, free text as they write it (one field, no first/last split); null rejected; trimmed, `MIN_LENGTH`..`MAX_LENGTH` (2..100) chars → `InvalidFullNameException` (Validation) |
| `RawPassword(String value)` | ≥ 8 chars and ≤ 72 **bytes** in UTF-8 (BCrypt's limit is in bytes: Spring Security 7.1.1 `BCrypt.hashpw` throws above it), at least one letter and one digit → `WeakPasswordException` ("at least 8 characters, including a letter and a digit"); over 72 bytes → `PasswordTooLongException` ("Password is too long": the byte limit is an implementation detail users can't act on). Both are Validation. **`toString()` is redacted.** Never stored and never logged |
| `PasswordHash(String value)` | non-blank. Opaque to the domain |

### `LocalPokemon` behaviour

| Method | Rule |
|---|---|
| `static create(LocalPokemonId, PokedexNumber, Instant now)` | Custom attributes start empty, `syncedAt = updatedAt = now` |
| `updateCustomAttributes(CustomAttributes, Instant now)` | Replaces all custom attributes (PUT semantics), `updatedAt = now` |
| `displayName(String canonicalName)` | `localizedName` if set, otherwise the canonical name the caller read from PokeAPI |
| `static builder()` | **Reconstitution only** (`LocalPokemonEntityMapper`) |

### `UserAccount` behaviour

| Method | Rule |
|---|---|
| `static register(UserId, Email, FullName, PasswordHash, Instant now)` | `createdAt = now`; exposed as `getName()` |
| `static builder()` | Reconstitution only |

Password policy runs on `RawPassword` **before** hashing. The domain never sees a hash being
computed: that happens behind the `PasswordHasher` port.

### Pokémon repository (`domain/repository`)

The domain never knows where data comes from: not JPA, not PokeAPI, not JSON. Every data port is a
**repository**: an interface in `domain/repository`, implemented in `infrastructure`, where the
technical format (a JPA entity, PokeAPI's JSON) is translated into domain types and never leaves
the adapter. Domain code doesn't name PokeAPI, not even in comments. `domain/model` holds the
types the repositories return and the aggregates. Whether a repository can write is said by its
interface alone:

| Repository | Methods | Backed by (infrastructure only) |
|---|---|---|
| `UserAccountRepository` | `save`, `findById`, `findByEmail` | PostgreSQL |
| `PokemonRepository` | `findAll`, `findByIdentifier` (read-only: the canonical data isn't ours to change) | PokeAPI |
| `LocalPokemonRepository` | `save`, `findByPokedexNumber` (+ `delete` in Slice 5) | PostgreSQL |

The canonical data and the local record are still two things in the domain, not because of where
they are stored but because of what the business allows: the canonical data can only be read, the
local record (`LocalPokemon`) is synced, edited and removed. Clients never see the split: the use
cases merge both into one Pokémon (D-030).

```java
public interface PokemonRepository {
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
| `PokemonDataUnavailableException extends DataUnavailableException` | The Pokémon repository's data can't be reached. See *Data unavailable* below |

Every method may throw `PokemonDataUnavailableException`.

### Repository ports (`domain/repository`)

```java
public interface LocalPokemonRepository {
    LocalPokemon save(LocalPokemon pokemon);                    // number taken → PokemonAlreadySyncedException
    Optional<LocalPokemon> findByPokedexNumber(PokedexNumber number);
    default LocalPokemon getByPokedexNumber(PokedexNumber number) {
        return findByPokedexNumber(number).orElseThrow(() -> new LocalPokemonNotFoundException(number));
    }
    // Slice 5: void delete(LocalPokemon pokemon);
    // Slice 6: List<LocalPokemon> findAllByPokedexNumbers(Collection<PokedexNumber> numbers);  // 1 query per page
}
```

The record has no name (D-039), so the repository is keyed by number only. A use case that gets a
name resolves it to a number through `PokemonRepository` first.
Every method may throw `LocalPokemonDataUnavailableException`.

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
│   └── InvalidEmailException, InvalidFullNameException, WeakPasswordException, PasswordTooLongException
└── UnauthenticatedException (abstract)             → 401 UNAUTHENTICATED
    ├── InvalidCredentialsException                 (same message for unknown email and wrong password)
    └── UnknownAccountException                     (valid token, but the account no longer exists — D-033)
```

**Data unavailable.** Any repository whose data can't be reached throws a subclass of the abstract
`DataUnavailableException` (not a `DomainException`: no business rule was broken):
`PokemonDataUnavailableException`, `UserAccountDataUnavailableException`,
`LocalPokemonDataUnavailableException`, and `TransactionUnavailableException` when not even a
transaction can start (or finish). Every one of them is a
**503 `DATA_UNAVAILABLE`** with the same neutral message ("The service is temporarily unavailable.
Please try again in a moment."); the specific class and its cause go to the log, which is where the
root cause is read. It doesn't matter whether the data sits in PostgreSQL or behind PokeAPI.

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
| `GetLocalPokemonUseCase` | `LocalPokemonOutput execute(GetLocalPokemonInput)` | US-03 | The resource `Location` points to. A number goes straight to the database; a name is resolved through `PokemonRepository` first. 404 if not synced |
| `UpdateLocalPokemonUseCase` | `LocalPokemonOutput execute(UpdateLocalPokemonInput, Instant now)` | US-04 | 404 not synced / 400 / 409 concurrent |
| `RemoveLocalPokemonUseCase` | `void execute(RemoveLocalPokemonInput)` | CRUD-D | 404 not synced |
| `RegisterUserUseCase` | `UserOutput execute(RegisterUserInput, UserId, Instant now)` | TR-AUTH | 409 on duplicate email |
| `AuthenticateUserUseCase` | `AccessTokenOutput execute(AuthenticateUserInput, Instant now)` | TR-AUTH | 401 `InvalidCredentialsException` |
| `GetCurrentUserUseCase` | `UserOutput execute(GetCurrentUserInput)` | TR-AUTH | Account gone (e.g. the database was reset while a token was still valid) → 401 `UnknownAccountException` |

The `/local` use cases (get-local, update, remove) go straight to the database for a **number**.
A **name** is resolved to its number through `PokemonRepository` first, because the record keeps no
name (D-039). PokeAPI's cache makes that lookup cheap after the detail page has loaded.

The auth input DTOs are wrapped by `UserAccountMapper` into `Registration(Email, FullName,
RawPassword)` and `Credentials(Email, RawPassword)` before any port is called, so malformed input is
a 400 and never reaches the hasher or the database. Register hashes **before** opening the
transaction (BCrypt is slow), then checks `findByEmail` (409) inside it; the unique constraint stays
the backstop under concurrency.

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
| `user_accounts` | `id uuid pk`, `email varchar(254) unique`, `name varchar(100)`, `password_hash`, `created_at timestamptz`, `version bigint not null` | `uk_user_accounts_email` → `EmailAlreadyRegisteredException` |

Migrations, in the order the slices create them: `V1__create_user_accounts.sql`,
`V2__create_local_pokemons.sql`, `V3__seed_demo_data.sql`. The seed has a demo user with a BCrypt hash, plus about 10 synced Pokémon
with custom attributes and tags, so the demo starts with merged data. **Pikachu is not in the
seed**, because it's synced live in the demo. The Pokédex numbers come from **real PokeAPI
responses** (recorded with `curl`, like the test fixtures), never typed from memory. The demo
credentials are written in the README and nowhere else.

---

## API contract

Base path **`/api/v1`**, a plain prefix (D-029). JSON only. Errors always use `ErrorResponse`, and
lists always use `PageResponse`. `{identifier}` is a name or a Pokédex number (`pikachu` or `25`).
The frontend doesn't know or care which data comes from PokeAPI and which from the database.

Routes not in this table need a token (D-035): without one, they answer 401, not 404. Public
routes ignore the `Authorization` header, so an expired token never makes them fail (D-036).

| Method & path | Auth | Success | Errors | Story |
|---|---|---|---|---|
| `GET /pokemon?page=0&size=20` | public | 200 `PageResponse<PokemonSummaryResponse>` (`displayName` and `synced` arrive with Slice 6) | 400 bad page/size, 503 `DATA_UNAVAILABLE` | US-01 |
| `GET /pokemon/{identifier}` | public | 200 `PokemonDetailResponse` | 400, 404, 503 | US-02 |
| `GET /pokemon/{identifier}/local` | public | 200 `LocalPokemonResponse` | 400, 404 (not in PokeAPI / not synced), 503 | US-03 |
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

// UserResponse — register (201) and /auth/me (200). Never the password hash.
{ "id": "6f1c…", "email": "ash@pallet.town", "name": "Ash Ketchum", "createdAt": "…" }

// LocalPokemonResponse — the /local sub-resource: our record alone, no PokeAPI data (D-039)
{ "pokedexNumber": 25, "localizedName": "Pikachu BR", "region": "Kanto",
  "tags": ["mascot", "starter"], "syncedAt": "…", "updatedAt": "…" }
```

**Validation (D-028; both kinds are 400, as US-04 requires).** The domain is the single validation
authority. Every rule (format, length, policy) lives in a VO (`Tag`, `CustomAttributes`,
`PokemonIdentifier`, `Email`, `FullName`, `RawPassword`) and throws a `ValidationException`
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
