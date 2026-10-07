# Application Layer — Examples

Reference code for `application/`, against the real model in
[`../domain-model.md`](../domain-model.md). Rules: [`../standards/backend.md`](../standards/backend.md).

**This layer is pure Java.** There's no `@Service`, `@Component`, `@Transactional` or logger, and
the imports are only `java.*`, `…domain.*` and `…application.*`. ArchUnit enforces this with an
allowlist. Spring meets this code in one place only: the composition root at the end of this file.

## Input port + Interactor — `SyncPokemonUseCase` (US-03)

The input port is an interface with one `execute`, and controllers depend on it. The interactor is
a plain class that implements it.

A mutating use case gets both ambient values as parameters: the new aggregate's **id** and **now**.
Both are produced in `interfaces/`.

```java
// application/usecase/SyncPokemonUseCase.java — input port
public interface SyncPokemonUseCase {
    LocalPokemonOutput execute(SyncPokemonInput input, LocalPokemonId id, Instant now);
}
```

```java
// application/usecase/SyncPokemonInteractor.java
public class SyncPokemonInteractor implements SyncPokemonUseCase {

    private final PokemonRepository source;
    private final LocalPokemonRepository repository;
    private final LocalPokemonMapper mapper;
    private final UnitOfWork unitOfWork;

    public SyncPokemonInteractor(PokemonRepository source,
                                 LocalPokemonRepository repository,
                                 LocalPokemonMapper mapper,
                                 UnitOfWork unitOfWork) {
        this.source = source;
        this.repository = repository;
        this.mapper = mapper;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public LocalPokemonOutput execute(SyncPokemonInput input, LocalPokemonId id, Instant now) {
        // 1. Remote call FIRST, outside the transaction. A PokeAPI round trip must never hold a
        //    DB connection/transaction open. 404 → PokemonNotFoundException,
        //    outage → PokemonDataUnavailableException; both just propagate.
        var identifier = mapper.toIdentifier(input.identifier());
        var detail = source.getByIdentifier(identifier);

        // 2. Read-check-write against our own store, atomically.
        return unitOfWork.inTransaction(() -> {
            // The unique constraint on pokedex_number is the real guarantee under concurrency (the
            // adapter translates a violation into the same exception). This check gives the
            // common case a clear 409 without relying on the constraint.
            repository.findByPokedexNumber(detail.number()).ifPresent(existing -> {
                throw new PokemonAlreadySyncedException(detail.number());
            });

            var pokemon = mapper.toDomain(detail, id, now);
            return LocalPokemonOutput.from(repository.save(pokemon));
        });
    }
}
```

That `ifPresent → throw` is **not** the forbidden "pre-validate and skip" pattern. It throws, so
the caller gets a 409 and can tell the outcome apart from a success. What's forbidden is a guard
that *returns quietly*.

## The merge — `GetPokemonUseCase` (US-02)

One request returns PokeAPI data merged with the local record. Merging is orchestration (one port,
one repository), so it belongs here. *What* the merged view says, such as which name to display,
is a domain rule on `LocalPokemon`. No transaction: nothing is written.

```java
// application/usecase/GetPokemonUseCase.java
public interface GetPokemonUseCase {
    PokemonDetailOutput execute(GetPokemonInput input);
}

// application/usecase/GetPokemonInteractor.java
public class GetPokemonInteractor implements GetPokemonUseCase {

    private final PokemonRepository source;
    private final LocalPokemonRepository repository;
    private final LocalPokemonMapper mapper;

    public GetPokemonInteractor(PokemonRepository source, LocalPokemonRepository repository,
                                LocalPokemonMapper mapper) {
        this.source = source;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public PokemonDetailOutput execute(GetPokemonInput input) {
        var detail = source.getByIdentifier(mapper.toIdentifier(input.identifier()));
        var local = repository.findByPokedexNumber(detail.number());   // absent = not synced, not an error
        return PokemonDetailOutput.from(detail, local);
    }
}
```

## The list merge — `BrowsePokemonUseCase` (US-01)

The same idea for a page. The local records for the whole page come in **one** query, never one
per Pokémon.

```java
// application/usecase/BrowsePokemonInteractor.java
public class BrowsePokemonInteractor implements BrowsePokemonUseCase {

    private final PokemonRepository source;
    private final LocalPokemonRepository repository;

    public BrowsePokemonInteractor(PokemonRepository source, LocalPokemonRepository repository) {
        this.source = source;
        this.repository = repository;
    }

    @Override
    public PageOutput<PokemonSummaryOutput> execute(BrowsePokemonInput input) {
        var pageRequest = new PageRequest(input.page(), input.size());
        var page = source.findAll(pageRequest);

        var numbers = page.content().stream().map(PokemonSummary::number).toList();
        var localByNumber = repository.findAllByPokedexNumbers(numbers).stream()
            .collect(Collectors.toMap(LocalPokemon::getPokedexNumber, Function.identity()));

        return PageOutput.from(
            page.map(summary -> PokemonSummaryOutput.from(summary,
                Optional.ofNullable(localByNumber.get(summary.number())))),
            pageRequest);
    }
}
```

`new PageRequest(...)` here is a deliberate, narrow exception to "construction lives in the
mapper". `PageRequest` is a query parameter, not a domain object being created from external
input, and it has no mapper of its own.

## Update — `UpdateLocalPokemonUseCase` (US-04)

404 when the Pokémon isn't synced (from `getByIdentifier`). 400 when an attribute is invalid
(thrown by the `Tag`/`CustomAttributes` constructors inside the mapper). 409 on a concurrent edit
(the adapter translates `@Version`). The interactor checks none of this itself, and never calls
PokeAPI.

```java
// application/usecase/UpdateLocalPokemonUseCase.java
public interface UpdateLocalPokemonUseCase {
    LocalPokemonOutput execute(UpdateLocalPokemonInput input, Instant now);
}

// application/usecase/UpdateLocalPokemonInteractor.java
public class UpdateLocalPokemonInteractor implements UpdateLocalPokemonUseCase {

    private final LocalPokemonRepository repository;
    private final LocalPokemonMapper mapper;
    private final UnitOfWork unitOfWork;

    public UpdateLocalPokemonInteractor(LocalPokemonRepository repository, LocalPokemonMapper mapper,
                                        UnitOfWork unitOfWork) {
        this.repository = repository;
        this.mapper = mapper;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public LocalPokemonOutput execute(UpdateLocalPokemonInput input, Instant now) {
        // Built BEFORE the transaction: invalid input fails fast with a 400 and never opens one.
        var identifier = mapper.toIdentifier(input.identifier());
        var attributes = mapper.toCustomAttributes(input);

        return unitOfWork.inTransaction(() -> {
            var pokemon = repository.getByIdentifier(identifier);
            pokemon.updateCustomAttributes(attributes, now);   // the domain decides
            return LocalPokemonOutput.from(repository.save(pokemon));
        });
    }
}
```

## Void use case — `RemoveLocalPokemonUseCase`

Nothing to return, so this uses the `Runnable` overload of `inTransaction` with a block lambda.

```java
// application/usecase/RemoveLocalPokemonInteractor.java
public class RemoveLocalPokemonInteractor implements RemoveLocalPokemonUseCase {

    private final LocalPokemonRepository repository;
    private final LocalPokemonMapper mapper;
    private final UnitOfWork unitOfWork;

    // constructor omitted

    @Override
    public void execute(RemoveLocalPokemonInput input) {
        var identifier = mapper.toIdentifier(input.identifier());
        unitOfWork.inTransaction(() -> {
            // getByIdentifier first: removing something that isn't synced must be a 404, not a
            // silent 204 that claims something was removed.
            var pokemon = repository.getByIdentifier(identifier);
            repository.delete(pokemon);
        });
    }
}
```

## Authentication — `AuthenticateUserUseCase` (TR-AUTH)

Hashing and token issuing are orchestration needs, so they sit behind **output ports** in
`application/port/`. The interactor never sees BCrypt or JWT.

```java
// application/port/PasswordHasher.java
public interface PasswordHasher {
    PasswordHash hash(RawPassword password);
    boolean matches(RawPassword candidate, PasswordHash hash);
}

// application/port/TokenIssuer.java
public interface TokenIssuer {
    AccessToken issue(UserAccount account, Instant now);
}

// application/port/AccessToken.java
public record AccessToken(String value, Instant expiresAt) {}
```

```java
// application/usecase/AuthenticateUserInteractor.java
public class AuthenticateUserInteractor implements AuthenticateUserUseCase {

    private final UserAccountRepository userAccountRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final UserAccountMapper userAccountMapper;

    public AuthenticateUserInteractor(UserAccountRepository userAccountRepository,
                                      PasswordHasher passwordHasher,
                                      TokenIssuer tokenIssuer,
                                      UserAccountMapper userAccountMapper) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public AccessTokenOutput execute(AuthenticateUserInput input, Instant now) {
        var credentials = userAccountMapper.toCredentials(input);

        // findByEmail (not getById-style throw): an unknown email must produce the SAME error as a
        // wrong password, so the API can't be used to enumerate registered emails.
        var account = userAccountRepository.findByEmail(credentials.email())
            .filter(found -> passwordHasher.matches(credentials.password(), found.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

        return AccessTokenOutput.from(tokenIssuer.issue(account, now));
    }
}
```

`UserAccountMapper.toCredentials(input)` returns `Credentials(Email email, RawPassword password)`, a
small record in `application/dto`. Wrapping the strings is the mapper's job, so a malformed email at
login is a 400, not a confusing 401.

## Transaction boundary — `UnitOfWork`

`application/port/`. This is what makes a use case "one atomic operation". It is explicit instead
of `@Transactional`, because annotations don't belong in this layer, and because the boundary then
shows up in the method body and can be mocked in a context-free test.

```java
// application/port/UnitOfWork.java
public interface UnitOfWork {

    <T> T inTransaction(Supplier<T> work);

    /** For use cases with nothing to return. Default method: adapters implement only the Supplier form. */
    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
```

**Always pass a block lambda** (`() -> { ... }`). A bare expression like
`() -> repository.save(p)` fits both overloads, and javac rejects it as ambiguous.

What the transaction does **not** give you:
- **Uniqueness.** Two concurrent syncs of #25 can both see "absent".
  `uk_local_pokemons_pokedex_number` plus the adapter's translation into
  `PokemonAlreadySyncedException` handles that.
- **Lost-update protection.** That comes from `@Version` on `LocalPokemonEntity`, translated into
  `LocalPokemonModifiedConcurrentlyException` (409).

## Mapper — `LocalPokemonMapper`

A plain class in `application/mapper/`. It owns every *external input → domain object*
construction: wrapping strings into VOs and calling `LocalPokemon.create`. It's a pure function of
its arguments (no ports), so it's never mocked. When it needs an id or `now`, it gets them as
parameters.

```java
// application/mapper/LocalPokemonMapper.java
public class LocalPokemonMapper {

    public PokemonIdentifier toIdentifier(String raw) {
        return new PokemonIdentifier(raw);
    }

    // id and now are parameters — this method can't generate or read either.
    public LocalPokemon toDomain(PokemonDetail detail, LocalPokemonId id, Instant now) {
        return LocalPokemon.create(id, detail.number(), detail.profile().toSnapshot(), now);
    }

    public CustomAttributes toCustomAttributes(UpdateLocalPokemonInput input) {
        var tags = input.tags() == null ? Set.<Tag>of()
            : input.tags().stream().map(Tag::new).collect(Collectors.toUnmodifiableSet());
        return new CustomAttributes(input.localizedName(), input.region(), tags);
    }
}
```

When it passes about four methods, split it per use case. A mapper that collects every
representation of an aggregate is a god class.

## Input / Output DTOs

Always a `record`. Outputs have a static `from(...)` factory, and no domain type ever leaves
through them: only primitives, `String`, `Instant`, `BigDecimal`, lists of those, and nested output
records. The merged outputs take the local record as an `Optional`, so "not synced" is explicit,
never a `null` passed around.

```java
// application/dto/UpdateLocalPokemonInput.java
public record UpdateLocalPokemonInput(String identifier, String localizedName, String region, List<String> tags) {}

// application/dto/LocalPokemonOutput.java — the /local sub-resource
public record LocalPokemonOutput(int pokedexNumber, String name, String displayName,
                                 String localizedName, String region, List<String> tags,
                                 Instant syncedAt, Instant updatedAt) {

    public static LocalPokemonOutput from(LocalPokemon pokemon) {
        var custom = pokemon.getCustomAttributes();
        return new LocalPokemonOutput(
            pokemon.getPokedexNumber().value(),
            pokemon.getSnapshot().name(),
            pokemon.displayName(),
            custom.localizedName(),
            custom.region(),
            custom.tags().stream().map(Tag::value).sorted().toList(), // deterministic order for clients/tests
            pokemon.getSyncedAt(),
            pokemon.getUpdatedAt());
    }
}

// application/dto/PokemonDetailOutput.java — PokeAPI data merged with the local record
public record PokemonDetailOutput(int pokedexNumber, String name, String displayName,
                                  String category, BigDecimal heightMeters, BigDecimal weightKilograms,
                                  String spriteUrl, String artworkUrl, List<String> types,
                                  List<AbilityOutput> abilities, List<StatOutput> stats,
                                  String description, EvolutionStageOutput evolutionChain,
                                  LocalAttributesOutput local) {   // null when not synced

    public static PokemonDetailOutput from(PokemonDetail detail, Optional<LocalPokemon> local) {
        var profile = detail.profile();
        return new PokemonDetailOutput(
            detail.number().value(),
            profile.name(),
            local.map(LocalPokemon::displayName).orElse(profile.name()),
            profile.category(),
            profile.height().meters(),
            profile.weight().kilograms(),
            profile.spriteUrl(),
            profile.artworkUrl(),
            profile.types().stream().map(PokemonType::name).toList(),
            profile.abilities().stream().map(AbilityOutput::from).toList(),
            profile.stats().stream().map(StatOutput::from).toList(),
            profile.description(),
            EvolutionStageOutput.from(detail.evolutionChain()),
            local.map(LocalAttributesOutput::from).orElse(null));
    }
}

// application/dto/PageOutput.java — application-level page, so interfaces/ never sees domain Page
public record PageOutput<T>(List<T> content, int page, int size, long totalElements) {

    public static <T> PageOutput<T> from(Page<T> page, PageRequest request) {
        return new PageOutput<>(page.content(), request.page(), request.size(), page.totalElements());
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }
}
```

`PokemonSummaryOutput.from(summary, local)` follows the same shape: `displayName` plus a `synced`
boolean.

## Composition root — `UseCaseConfig`

This is where Spring meets the pure-Java core. It's the "Main" component of Clean Architecture:
the only class that knows concrete interactors. It lives in `infrastructure/config/` because it's
the one place allowed to see every layer. Each `@Bean` method **returns the input-port type**, so
nothing else in the context can ask for an `*Interactor`.

```java
// infrastructure/config/UseCaseConfig.java
@Configuration
public class UseCaseConfig {

    @Bean
    LocalPokemonMapper localPokemonMapper() {
        return new LocalPokemonMapper();
    }

    @Bean
    UserAccountMapper userAccountMapper() {
        return new UserAccountMapper();
    }

    @Bean
    BrowsePokemonUseCase browsePokemonUseCase(PokemonRepository source, LocalPokemonRepository repository) {
        return new BrowsePokemonInteractor(source, repository);
    }

    @Bean
    SyncPokemonUseCase syncPokemonUseCase(PokemonRepository source, LocalPokemonRepository repository,
                                          LocalPokemonMapper mapper, UnitOfWork unitOfWork) {
        return new SyncPokemonInteractor(source, repository, mapper, unitOfWork);
    }

    @Bean
    UpdateLocalPokemonUseCase updateLocalPokemonUseCase(LocalPokemonRepository repository,
                                                        LocalPokemonMapper mapper, UnitOfWork unitOfWork) {
        return new UpdateLocalPokemonInteractor(repository, mapper, unitOfWork);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(UserAccountRepository repository,
                                                    PasswordHasher passwordHasher,
                                                    TokenIssuer tokenIssuer,
                                                    UserAccountMapper mapper) {
        return new AuthenticateUserInteractor(repository, passwordHasher, tokenIssuer, mapper);
    }

    // ... one @Bean per input port.
}
```

`ApplicationContextIT` (a `@SpringBootTest` context-load) proves that every `*UseCase` interface
resolves to a bean. A forgotten `@Bean` fails there, not in production.

## Read model — none planned

`application/query/{Name}Query` → `{Name}View` is for screens that join several aggregates. Nothing
here needs one: the merged views combine the PokeAPI port with **one** aggregate, which a use case
does with the port and the repository directly.
