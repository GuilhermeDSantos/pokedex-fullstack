# Infrastructure Layer — Examples

Reference code for `infrastructure/`, against the real model in
[`../domain-model.md`](../domain-model.md). Rules: [`../standards/backend.md`](../standards/backend.md).
This is where the frameworks live: JPA, Spring transactions, `RestClient`, cache, BCrypt, JWT.
None of their types leave this layer.

## Migrations — Flyway owns the schema

**The DDL lives in exactly one place: `src/main/resources/db/migration/`.** This doc doesn't copy
it, because a second copy would drift on the first new migration. Hibernate never creates or alters
anything (`ddl-auto: validate`). It only checks that the entities match what Flyway built.

Conventions every migration follows:

- File name `V{n}__{snake_case_description}.sql`. Plain SQL. **Never edited once committed.** A fix
  is a new migration.
- Name every constraint explicitly: `pk_…`, `uk_{table}_{column}`, `fk_{table}_{ref}`,
  `ck_{table}_{rule}`, `ix_{table}_{column}`. The repository adapter recognises
  `uk_local_pokemons_pokedex_number` when translating a `DataIntegrityViolationException` into
  `PokemonAlreadySyncedException`, and a generated name would make that brittle.
- Tables of an aggregate's **child values** reference the root with `ON DELETE CASCADE`. Deleting
  the root deletes its values, so the DB and the domain agree on what "one local Pokémon" is.
- Every table backing an `@Entity` has `version BIGINT NOT NULL DEFAULT 0`.
- Seed/demo data is its own migration (`V3__seed_demo_data.sql`) with fixed UUIDs and a BCrypt
  hash, never a plaintext password.
- The column layout planned for each table is in [`../domain-model.md`](../domain-model.md#persistence-postgresql-flyway-owned)
  as *design intent*. Once the migration exists, **the migration is the truth**.

Illustrating only the conventions, not the real schema:

```sql
-- shape of a child-value table: composite PK, cascade from the root, named constraints
CREATE TABLE local_pokemon_tags (
    local_pokemon_id UUID        NOT NULL,
    tag              VARCHAR(30) NOT NULL,
    CONSTRAINT pk_local_pokemon_tags PRIMARY KEY (local_pokemon_id, tag),
    CONSTRAINT fk_local_pokemon_tags_local_pokemon
        FOREIGN KEY (local_pokemon_id) REFERENCES local_pokemons (id) ON DELETE CASCADE
);
```

The repository IT runs against these real migrations (Testcontainers Postgres), so a mismatch
between entity and migration fails the build at Hibernate validation.

## JPA Entity

Only in `infrastructure/persistence/entity/`. Things to notice:
- The snapshot is plain columns (D-031). The only child collection is the tags, an
  `@ElementCollection`: values inside the aggregate, with no identity and no repository.
- There's no `@ManyToOne`/`@OneToOne`/`@ManyToMany` anywhere, and ArchUnit bans them.
- `@Version` is mandatory.
- The constructor is `public`, because the mapper lives in a sibling package.

```java
// infrastructure/persistence/entity/LocalPokemonEntity.java
@Entity
@Table(name = "local_pokemons")
public class LocalPokemonEntity {

    @Id
    private UUID id;

    @Column(name = "pokedex_number", nullable = false, unique = true)
    private int pokedexNumber;

    @Column(nullable = false, unique = true)
    private String name;

    private String category;

    @Column(name = "height_m", nullable = false)
    private BigDecimal heightMeters;

    @Column(name = "weight_kg", nullable = false)
    private BigDecimal weightKilograms;

    @Column(name = "sprite_url")
    private String spriteUrl;

    @Column(name = "artwork_url")
    private String artworkUrl;

    private String description;

    @Column(name = "localized_name")
    private String localizedName;

    private String region;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "local_pokemon_tags", joinColumns = @JoinColumn(name = "local_pokemon_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public LocalPokemonEntity() {} // JPA; public for the mapper in a sibling package

    // Getters and setters — acceptable ONLY on JPA entities. (Omitted for brevity.)
}
```

Tags are `EAGER`: bounded (≤ 10) and always shown. For the list merge, which loads up to 50 records
at once, set `hibernate.default_batch_fetch_size` so the page's tags load in one extra query,
not one per record.

## Spring Data interface

`{Name}JpaRepository`. Spring Data's own types are fine here, because this interface never leaves
infrastructure.

```java
// infrastructure/persistence/repository/LocalPokemonJpaRepository.java
public interface LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, UUID> {

    Optional<LocalPokemonEntity> findByPokedexNumber(int pokedexNumber);

    Optional<LocalPokemonEntity> findByName(String name);

    List<LocalPokemonEntity> findAllByPokedexNumberIn(Collection<Integer> pokedexNumbers);
}
```

## Repository adapter

`Jpa{Name}Repository` implements the domain port. It does three jobs: mapping (via the entity
mapper), resolving identifiers to queries, and **exception translation**, so nothing Spring-shaped
escapes. There's no `@Transactional`: the boundary is the `UnitOfWork` the interactor opened.

```java
// infrastructure/persistence/repository/JpaLocalPokemonRepository.java
@Repository
public class JpaLocalPokemonRepository implements LocalPokemonRepository {

    private final LocalPokemonJpaRepository jpaRepository;
    private final LocalPokemonEntityMapper mapper;

    public JpaLocalPokemonRepository(LocalPokemonJpaRepository jpaRepository, LocalPokemonEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public LocalPokemon save(LocalPokemon pokemon) {
        try {
            // Update the MANAGED entity when one exists, so Hibernate keeps the version it read and
            // a concurrent write in between is detected. A detached `new LocalPokemonEntity()` would
            // carry version 0 and either fail spuriously or overwrite blindly.
            var entity = jpaRepository.findById(pokemon.getId().value())
                .map(existing -> mapper.copyInto(pokemon, existing))
                .orElseGet(() -> mapper.toEntity(pokemon));
            // saveAndFlush: surface constraint/version violations HERE, where they can be
            // translated, not later at commit time inside SpringUnitOfWork.
            return mapper.toDomain(jpaRepository.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException e) {
            throw new LocalPokemonModifiedConcurrentlyException(pokemon.getPokedexNumber());
        } catch (DataIntegrityViolationException e) {
            // Translate only the violation we know; anything else is a bug and must stay loud.
            if (violates(e, "uk_local_pokemons_pokedex_number")) {
                throw new PokemonAlreadySyncedException(pokemon.getPokedexNumber());
            }
            throw e;
        }
    }

    @Override
    public Optional<LocalPokemon> findByPokedexNumber(PokedexNumber number) {
        return jpaRepository.findByPokedexNumber(number.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<LocalPokemon> findByIdentifier(PokemonIdentifier identifier) {
        var entity = identifier.isNumber()
            ? jpaRepository.findByPokedexNumber(identifier.asNumber().value())
            : jpaRepository.findByName(identifier.value());
        return entity.map(mapper::toDomain);
    }

    @Override
    public List<LocalPokemon> findAllByPokedexNumbers(Collection<PokedexNumber> numbers) {
        if (numbers.isEmpty()) {
            return List.of();   // explicit: an empty IN () is invalid SQL on some databases
        }
        var values = numbers.stream().map(PokedexNumber::value).toList();
        return jpaRepository.findAllByPokedexNumberIn(values).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(LocalPokemon pokemon) {
        jpaRepository.deleteById(pokemon.getId().value());
    }
}
```

**About `@Version`.** The domain `LocalPokemon` carries no JPA version. The adapter above updates
the managed entity it loads inside the same `UnitOfWork`, so the version that was read is the one
checked on flush, and two overlapping transactions on the same record produce a 409 instead of a
lost update (D-011). Exposing the version to clients (`If-Match` on PUT), so that a stale browser
tab also gets a 409, is parked as a next step.

## Entity mapper

`@Component`: `toDomain`, `toEntity` (new rows) and `copyInto` (managed rows). **It carries the whole
aggregate, tags included.** A mapper that forgets the tags compiles, passes a scalar-only test, and
silently empties every record on save. The repository IT asserts the full aggregate.

```java
// infrastructure/persistence/mapper/LocalPokemonEntityMapper.java
@Component
public class LocalPokemonEntityMapper {

    public LocalPokemon toDomain(LocalPokemonEntity entity) {
        var snapshot = new PokemonSnapshot(
            entity.getName(),
            entity.getCategory(),
            new Height(entity.getHeightMeters()),
            new Weight(entity.getWeightKilograms()),
            entity.getSpriteUrl(),
            entity.getArtworkUrl(),
            entity.getDescription());

        var custom = new CustomAttributes(
            entity.getLocalizedName(),
            entity.getRegion(),
            entity.getTags().stream().map(Tag::new).collect(Collectors.toUnmodifiableSet()));

        // builder(), not create(): reconstitution restores stored state, it doesn't re-run
        // creation rules (create() would wipe custom attributes and reset syncedAt).
        return LocalPokemon.builder()
            .id(new LocalPokemonId(entity.getId()))
            .pokedexNumber(new PokedexNumber(entity.getPokedexNumber()))
            .snapshot(snapshot)
            .customAttributes(custom)
            .syncedAt(entity.getSyncedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public LocalPokemonEntity toEntity(LocalPokemon pokemon) {
        var entity = new LocalPokemonEntity();
        entity.setId(pokemon.getId().value());
        return copyInto(pokemon, entity);
    }

    /** Copies every field of the aggregate onto a (possibly managed) entity. */
    public LocalPokemonEntity copyInto(LocalPokemon pokemon, LocalPokemonEntity entity) {
        // Mirror image of toDomain. For the tags: clear() + addAll() on the existing set, never
        // setTags(new HashSet<>()) — Hibernate tracks the collection instance it gave you.
        // (Omitted for brevity.)
        return entity;
    }
}
```

## Unit of Work adapter

`infrastructure/transaction/SpringUnitOfWork`, the **only** transaction boundary in the project.
It's based on `TransactionTemplate`, so it's a real call rather than a proxy. That means it works
from anywhere, self-invocation included, and it needs no `@Transactional` at all.

```java
// infrastructure/transaction/SpringUnitOfWork.java
@Component
public class SpringUnitOfWork implements UnitOfWork {

    private final TransactionTemplate template;

    public SpringUnitOfWork(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
    // inTransaction(Runnable) is the port's default method, delegating here.
}
```

## PokeAPI adapter

`infrastructure/external/pokeapi/`. Two beans: `PokeApiClient` does the cached HTTP, one method per
PokeAPI resource, and `PokeApiPokemonSource` implements the domain's `PokemonSource` port on top of
it. PokeAPI JSON shapes are **package-private records** in this package, so they can't leak.

```java
// infrastructure/external/pokeapi/PokeApiProperties.java
@ConfigurationProperties("pokeapi")
public record PokeApiProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout, int maxConcurrency) {}

// infrastructure/external/pokeapi/PokeApiPokemonJson.java — only the fields we use
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiPokemonJson(int id, String name, int height, int weight, Sprites sprites,
                          List<TypeSlot> types, List<AbilitySlot> abilities, List<StatEntry> stats,
                          NamedResource species) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Sprites(@JsonProperty("front_default") String frontDefault, Other other) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Other(@JsonProperty("official-artwork") Artwork officialArtwork) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Artwork(@JsonProperty("front_default") String frontDefault) {}

    record TypeSlot(int slot, NamedResource type) {}
    record AbilitySlot(int slot, @JsonProperty("is_hidden") boolean hidden, NamedResource ability) {}
    record StatEntry(@JsonProperty("base_stat") int baseStat, NamedResource stat) {}
}
```

### `PokeApiClient` — the cached HTTP calls

```java
// infrastructure/external/pokeapi/PokeApiClient.java
@Component
public class PokeApiClient {

    private static final Logger log = LoggerFactory.getLogger(PokeApiClient.class);

    private final RestClient restClient;   // builder.baseUrl(pokeapi.base-url); timeouts: spring.http.clients.* (D-038)

    // constructor omitted

    @Cacheable(cacheNames = "pokeapi-pages", key = "#offset + ':' + #limit")
    public PokeApiPageJson fetchPage(long offset, int limit) {
        return getRequired("/pokemon?offset={offset}&limit={limit}", PokeApiPageJson.class, offset, limit);
    }

    /** Empty when PokeAPI answers 404: an unknown name or number is a business fact, not an outage. */
    @Cacheable(cacheNames = "pokeapi-pokemon", key = "#identifier")
    public Optional<PokeApiPokemonJson> fetchPokemon(String identifier) {
        return get("/pokemon/{identifier}", PokeApiPokemonJson.class, identifier);
    }

    // Species and chain are followed by the URL the previous response gave us: alternate forms
    // (ids > 10000) have a species id that differs from the pokemon id. A 404 here means PokeAPI
    // contradicted itself, so it's treated as unavailable, not as "not found".
    @Cacheable(cacheNames = "pokeapi-species", key = "#url")
    public PokeApiSpeciesJson fetchSpecies(String url) {
        return getRequired(url, PokeApiSpeciesJson.class);
    }

    @Cacheable(cacheNames = "pokeapi-evolution-chains", key = "#url")
    public PokeApiEvolutionChainJson fetchEvolutionChain(String url) {
        return getRequired(url, PokeApiEvolutionChainJson.class);
    }

    /** 404 → empty; everything else that isn't a 2xx → PokemonSourceUnavailableException. */
    private <T> Optional<T> get(String uri, Class<T> type, Object... vars) {
        try {
            return Optional.ofNullable(restClient.get().uri(uri, vars).retrieve().body(type));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("PokeAPI call failed: {}", uri);   // path only — never the body
            throw new PokemonSourceUnavailableException("PokeAPI is unavailable right now", e);
        }
    }

    private <T> T getRequired(String uri, Class<T> type, Object... vars) {
        return get(uri, type, vars)
            .orElseThrow(() -> new PokemonSourceUnavailableException("PokeAPI returned incomplete data"));
    }
}
```

### `PokeApiPokemonSource` — the port adapter

No `@Cacheable` here, on purpose. Interactors call `getByIdentifier`, the port's `default` method,
which calls `this.findByIdentifier` *inside* the target object and bypasses Spring's caching proxy.
An annotation on `findByIdentifier` would never hit for detail or sync, and per-entry caching inside
`findAll` would fail the same way. Every call into `PokeApiClient` crosses a bean boundary, so it
goes through the proxy and is cached.

```java
// infrastructure/external/pokeapi/PokeApiPokemonSource.java
@Component
public class PokeApiPokemonSource implements PokemonSource {

    private final PokeApiClient client;           // cached HTTP: calls into it cross the proxy
    private final PokeApiTranslator translator;   // pure JSON → domain translation, unit-tested on its own
    private final ExecutorService executor;        // virtual threads, bounded by a semaphore (maxConcurrency)

    // constructor omitted

    @Override
    public Optional<PokemonDetail> findByIdentifier(PokemonIdentifier identifier) {
        return client.fetchPokemon(identifier.value()).map(pokemon -> {
            var species = client.fetchSpecies(pokemon.species().url());
            var chain = client.fetchEvolutionChain(species.evolutionChain().url());
            return translator.toDetail(pokemon, species, chain);
        });
    }

    // findAll(PageRequest): client.fetchPage(offset, size) → for each entry, CONCURRENTLY on the
    // executor: client.fetchPokemon + client.fetchSpecies (each cached by the client) →
    // translator.toSummary(...) → domain Page.
    // getByIdentifier is inherited from the port unchanged: the caching already happens in the client.
}
```

The translation rules (unit conversion, English genus, normalized flavor text, the recursive
evolution tree, slot ordering, null sprites) live in `PokeApiTranslator`. It's a pure class tested
with plain JUnit against recorded JSON in `src/test/resources/pokeapi/`. The HTTP behaviour of
`PokeApiClient` (404 → empty, 500/timeout → `PokemonSourceUnavailableException`) is tested
separately with `@RestClientTest` + `MockRestServiceServer`. The cache test calls
**`PokemonSource.getByIdentifier`** twice and asserts the second call makes no HTTP request, which
is exactly the path that the self-invocation trap would break.

### Cache configuration

```yaml
# application.yaml
spring:
  cache:
    type: caffeine
    cache-names: pokeapi-pages, pokeapi-pokemon, pokeapi-species, pokeapi-evolution-chains
    caffeine:
      spec: maximumSize=2000,expireAfterWrite=6h   # PokeAPI data is effectively static
```

`@EnableCaching` goes in `infrastructure/config/CacheConfig`. `@Cacheable` is allowed **only** on
`PokeApiClient`, never in `application`, which wouldn't compile anyway since it's framework-free.
An `Optional.empty()` result is fine to cache (a 404 is stable), but an exception is never cached.
Local data is never cached: it changes on every edit, and reading it is one indexed query.

## Security adapters

Each one implements an `application/port` interface. The interactors never see Spring Security.

```java
// infrastructure/security/BCryptPasswordHasher.java
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(RawPassword password) {
        return new PasswordHash(encoder.encode(password.value()));
    }

    @Override
    public boolean matches(RawPassword candidate, PasswordHash hash) {
        return encoder.matches(candidate.value(), hash.value());
    }
}

// infrastructure/security/JwtTokenIssuer.java
@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    // constructor omitted

    @Override
    public AccessToken issue(UserAccount account, Instant now) {
        var expiresAt = now.plus(properties.ttl());
        var claims = JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(account.getId().value().toString())
            .claim("email", account.getEmail().value())
            .issuedAt(now)             // `now` came from the edge — this adapter doesn't read the clock either
            .expiresAt(expiresAt)
            .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new AccessToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiresAt);
    }
}
```

`SecurityConfig` (`infrastructure/config/`) declares the `SecurityFilterChain`: stateless, CSRF off,
no CORS (same origin, D-019), `oauth2ResourceServer(jwt)`, and the route policy from
[`../domain-model.md#api-contract`](../domain-model.md#api-contract) (D-030):

```java
private static final RequestMatcher PUBLIC_ROUTES = new OrRequestMatcher(
    withDefaults().matcher("/actuator/health"),
    withDefaults().matcher("/actuator/health/**"),
    withDefaults().matcher(HttpMethod.GET, "/api/v1/pokemon/**"),
    withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/register"),
    withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/login"));

.authorizeHttpRequests(routes -> routes
    .requestMatchers(PUBLIC_ROUTES).permitAll()
    .anyRequest().authenticated())   // closed by default (D-035)
.oauth2ResourceServer(resourceServer -> resourceServer
    .bearerTokenResolver(ignoringPublicRoutes())   // D-036: an expired token can't break a public page
    .jwt(Customizer.withDefaults())
    .authenticationEntryPoint(authenticationEntryPoint))
```

For 401 it injects Spring Security's own `AuthenticationEntryPoint` **interface**. The
implementation that writes the standard `ErrorResponse` JSON lives in `interfaces/rest/security/`
(see [`rest.md`](rest.md#security-error-writers)), so `infrastructure` never imports `interfaces`.
It's registered on `oauth2ResourceServer(...)`, which also makes it the default entry point for a
missing token. Registering it only in `exceptionHandling(...)` misses invalid and expired tokens
(proven by mutation in `SecurityConfigIT`).
