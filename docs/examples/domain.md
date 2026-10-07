# Domain Layer — Examples

Reference code for `domain/`, written against this project's real model
([`../domain-model.md`](../domain-model.md)). The rules are in
[`../standards/backend.md`](../standards/backend.md). Imports are omitted. **Every** import in this
layer is `java.*` or `dev.guilhermeds.backend.domain.*`, and ArchUnit fails the build otherwise.

## Aggregate root — `LocalPokemon`

`create` makes new objects and `builder()` rebuilds them from persistence. There are no setters:
state changes only through behaviour methods. Identity is the `id`.

`create` does **not** generate the id or read the clock. Both arrive as parameters from
`interfaces/`. If `LocalPokemonId.generate()` ran in here, no test could assert the id of the record
it just synced.

```java
// domain/model/LocalPokemon.java

/**
 * A Pokémon synced from PokeAPI into the local database, carrying the organization's own fields.
 * Shared: one record per Pokémon, with no owner.
 *
 * <p>Invariants:
 * <ul>
 *   <li>id, pokedexNumber and snapshot are mandatory and never change after the sync</li>
 *   <li>a newly synced record has empty custom attributes and syncedAt == updatedAt == now</li>
 *   <li>updating custom attributes never touches the snapshot</li>
 * </ul>
 *
 * <p>Use {@link #create} for a new sync and {@link #builder()} to rebuild from persistence.
 *
 * <p><strong>{@link #builder()} is the reconstitution contract, for {@code LocalPokemonEntityMapper}
 * only.</strong> It null-checks but does not re-run creation rules, because what it rebuilds was
 * valid when stored. Business code never assembles a LocalPokemon through it.
 */
public class LocalPokemon {

    private final LocalPokemonId id;
    private final PokedexNumber pokedexNumber;
    private final PokemonSnapshot snapshot;
    private CustomAttributes customAttributes;
    private final Instant syncedAt;
    private Instant updatedAt;

    private LocalPokemon(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id must not be null");
        this.pokedexNumber = Objects.requireNonNull(builder.pokedexNumber, "pokedexNumber must not be null");
        this.snapshot = Objects.requireNonNull(builder.snapshot, "snapshot must not be null");
        this.customAttributes = Objects.requireNonNull(builder.customAttributes, "customAttributes must not be null");
        this.syncedAt = Objects.requireNonNull(builder.syncedAt, "syncedAt must not be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "updatedAt must not be null");
    }

    // -------------------------------------------------------------------------
    // Factory / Builder
    // -------------------------------------------------------------------------

    /** A new sync from PokeAPI (US-03): custom attributes start empty. */
    public static LocalPokemon create(LocalPokemonId id, PokedexNumber pokedexNumber,
                                      PokemonSnapshot snapshot, Instant now) {
        return builder()
            .id(id)
            .pokedexNumber(pokedexNumber)
            .snapshot(snapshot)
            .customAttributes(CustomAttributes.empty())
            .syncedAt(now)
            .updatedAt(now)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // Domain behaviour
    // -------------------------------------------------------------------------

    /** PUT semantics: the given attributes replace the current ones entirely (US-04). */
    public void updateCustomAttributes(CustomAttributes attributes, Instant now) {
        this.customAttributes = Objects.requireNonNull(attributes, "attributes must not be null");
        this.updatedAt = now;
    }

    /** The name the organization shows: its localized name when set, otherwise PokeAPI's. */
    public String displayName() {
        return customAttributes.localizedName() != null ? customAttributes.localizedName() : snapshot.name();
    }

    // -------------------------------------------------------------------------
    // Accessors (no setters)
    // -------------------------------------------------------------------------

    public LocalPokemonId getId() { return id; }
    public PokedexNumber getPokedexNumber() { return pokedexNumber; }
    public PokemonSnapshot getSnapshot() { return snapshot; }
    public CustomAttributes getCustomAttributes() { return customAttributes; }
    public Instant getSyncedAt() { return syncedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof LocalPokemon other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "LocalPokemon{id=" + id.value() + ", pokedexNumber=" + pokedexNumber.value() + "}";
    }

    // -------------------------------------------------------------------------
    // Builder — reconstitution only
    // -------------------------------------------------------------------------

    public static final class Builder {

        private LocalPokemonId id;
        private PokedexNumber pokedexNumber;
        private PokemonSnapshot snapshot;
        private CustomAttributes customAttributes;
        private Instant syncedAt;
        private Instant updatedAt;

        private Builder() {}

        public Builder id(LocalPokemonId id) { this.id = id; return this; }
        public Builder pokedexNumber(PokedexNumber pokedexNumber) { this.pokedexNumber = pokedexNumber; return this; }
        public Builder snapshot(PokemonSnapshot snapshot) { this.snapshot = snapshot; return this; }
        public Builder customAttributes(CustomAttributes customAttributes) { this.customAttributes = customAttributes; return this; }
        public Builder syncedAt(Instant syncedAt) { this.syncedAt = syncedAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public LocalPokemon build() {
            return new LocalPokemon(this);
        }
    }
}
```

The builder's methods are named after fields (`id(...)`), not `setId(...)`, so the
`no_setters_in_domain` ArchUnit rule (name-based) stays satisfied without exceptions.

`displayName()` is a business rule ("what name do we show?"), so it lives here, and the output
DTOs only call it. Neither the controller nor the frontend decides it.

## Value Objects

Always a `record`, validated in the compact constructor, immutable. Collections are copied with
`List.copyOf`/`Set.copyOf`, so nobody can mutate them through a reference they kept.

```java
// domain/model/LocalPokemonId.java
public record LocalPokemonId(UUID value) {

    public LocalPokemonId {
        Objects.requireNonNull(value, "local pokemon id must not be null");
    }

    /** The one home of UUID.randomUUID() for this id. Called only from interfaces/ (by review). */
    public static LocalPokemonId generate() {
        return new LocalPokemonId(UUID.randomUUID());
    }
}

// domain/model/PokedexNumber.java
public record PokedexNumber(int value) {

    public static final int MIN_VALUE = 1;

    public PokedexNumber {
        if (value < MIN_VALUE) {
            throw new InvalidPokedexNumberException(MIN_VALUE);
        }
    }
}

// domain/model/PokemonIdentifier.java — what a client puts in the URL: a name or a Pokédex number
public record PokemonIdentifier(String value) {

    public static final int MAX_LENGTH = 100;
    private static final Pattern FORMAT = Pattern.compile("^[a-z0-9-]+$");

    public PokemonIdentifier {
        Objects.requireNonNull(value, "identifier must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidPokemonIdentifierException(value);
        }
    }

    public boolean isNumber() {
        return value.chars().allMatch(Character::isDigit);
    }

    /** Only valid when {@link #isNumber()}; "0" is rejected by PokedexNumber itself (400). */
    public PokedexNumber asNumber() {
        if (!isNumber()) {
            throw new IllegalStateException("identifier is a name, not a number: " + value);
        }
        return new PokedexNumber(Integer.parseInt(value));
    }
}

// domain/model/Weight.java — the hectogram → kg conversion lives with the concept, not in an adapter
public record Weight(BigDecimal kilograms) {

    public Weight {
        Objects.requireNonNull(kilograms, "kilograms must not be null");
        if (kilograms.signum() < 0) {
            throw new IllegalArgumentException("weight cannot be negative");
        }
        kilograms = kilograms.setScale(1, RoundingMode.HALF_UP);
    }

    public static Weight fromHectograms(int hectograms) {
        return new Weight(BigDecimal.valueOf(hectograms).movePointLeft(1));
    }
}

// domain/model/PokemonSnapshot.java — the scalar part of PokeAPI's data, copied at sync time (D-031)
public record PokemonSnapshot(String name, String category, Height height, Weight weight,
                              String spriteUrl, String artworkUrl, String description) {
    public PokemonSnapshot {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(height, "height must not be null");
        Objects.requireNonNull(weight, "weight must not be null");
        // category, urls and description are nullable: PokeAPI has gaps
    }
}

// domain/model/Tag.java
public record Tag(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[a-z0-9][a-z0-9-]{0,29}$");

    public Tag {
        Objects.requireNonNull(value, "tag must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidTagException(value);
        }
    }
}

// domain/model/CustomAttributes.java — the proprietary fields of US-03/US-04, as one value
public record CustomAttributes(String localizedName, String region, Set<Tag> tags) {

    public static final int MAX_TAGS = 10;
    public static final int MAX_TEXT_LENGTH = 100;

    public CustomAttributes {
        localizedName = normalize(localizedName, "localizedName");
        region = normalize(region, "region");
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        if (tags.size() > MAX_TAGS) {
            throw new InvalidCustomAttributesException("a Pokémon can have at most " + MAX_TAGS + " tags");
        }
    }

    public static CustomAttributes empty() {
        return new CustomAttributes(null, null, Set.of());
    }

    // Optional text: blank means "not set". Explicit null, never an empty string in the DB.
    private static String normalize(String text, String field) {
        if (text == null || text.isBlank()) {
            return null;
        }
        var trimmed = text.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new InvalidCustomAttributesException(field + " must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        return trimmed;
    }
}

// domain/model/RawPassword.java — a secret: validated, never stored, never printed
public record RawPassword(String value) {

    public static final int MIN_LENGTH = 8;
    // BCrypt rejects inputs longer than 72 bytes, so the limit is in bytes, not characters.
    public static final int MAX_BYTES = 72;

    public RawPassword {
        if (value == null
            || value.length() < MIN_LENGTH
            || value.chars().noneMatch(Character::isLetter)
            || value.chars().noneMatch(Character::isDigit)) {
            throw new WeakPasswordException(MIN_LENGTH);
        }
        // Its own message: users can't count bytes, so "too long" is all they need to know.
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new PasswordTooLongException();
        }
    }

    @Override
    public String toString() {
        return "RawPassword[****]";
    }
}
```

`Weight` throws a plain `IllegalArgumentException` on a negative value, not a `ValidationException`.
That's on purpose: a weight only ever comes from PokeAPI, never from a user, so a negative one is a
programming/integration bug (500), not a client error (400). User-facing values (`Tag`,
`CustomAttributes`, `PokemonIdentifier`, `RawPassword`, `Email`) throw `ValidationException`
subtypes.

`PokemonProfile` (the full PokeAPI view, with types, abilities and stats) follows the same rules and
adds `toSnapshot()`, which returns the scalar subset the local record keeps.

## Pagination

`domain/pagination/`, not `domain/model/`. These records aren't ubiquitous language.

```java
// domain/pagination/PageRequest.java
public record PageRequest(int page, int size) {

    public static final int MIN_SIZE = 1;
    // Each item of a PokeAPI list page costs two upstream calls.
    public static final int MAX_SIZE = 50;

    public PageRequest {
        if (page < 0) {
            throw InvalidPageRequestException.negativePage();
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw InvalidPageRequestException.sizeOutOfRange(MIN_SIZE, MAX_SIZE);
        }
    }

    public long offset() {
        return (long) page * size;
    }
}

// domain/pagination/Page.java
public record Page<T>(List<T> content, long totalElements) {

    public Page {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(content.stream().map(mapper).toList(), totalElements);
    }
}
```

## Repository port

One per aggregate root. There's no `TagRepository`: tags are loaded and saved through
`LocalPokemon`.

```java
// domain/repository/LocalPokemonRepository.java
public interface LocalPokemonRepository {

    LocalPokemon save(LocalPokemon pokemon);

    Optional<LocalPokemon> findByPokedexNumber(PokedexNumber number);

    /** A number looks up pokedex_number, a name looks up name. Never calls PokeAPI. */
    Optional<LocalPokemon> findByIdentifier(PokemonIdentifier identifier);

    /** Same as findByIdentifier, but throws — the shape the update/remove use cases want. */
    default LocalPokemon getByIdentifier(PokemonIdentifier identifier) {
        return findByIdentifier(identifier).orElseThrow(() -> new LocalPokemonNotFoundException(identifier));
    }

    /** The list merge: the local records for one page of PokeAPI results, in a single query. */
    List<LocalPokemon> findAllByPokedexNumbers(Collection<PokedexNumber> numbers);

    void delete(LocalPokemon pokemon);
}
```

## PokeAPI port — `PokemonRepository`

PokeAPI as the domain sees it. It's declared here because "Pokémon can be browsed and looked up at
the source" is domain vocabulary, and sync builds the aggregate from it. Nothing HTTP-shaped leaks
in: no status codes, no JSON, no URLs except data fields.

```java
// domain/repository/PokemonRepository.java
public interface PokemonRepository {

    /** @throws PokemonDataUnavailableException when PokeAPI can't be reached */
    Page<PokemonSummary> findAll(PageRequest pageRequest);

    /** @throws PokemonDataUnavailableException when PokeAPI can't be reached */
    Optional<PokemonDetail> findByIdentifier(PokemonIdentifier identifier);

    default PokemonDetail getByIdentifier(PokemonIdentifier identifier) {
        return findByIdentifier(identifier)
            .orElseThrow(() -> new PokemonNotFoundException(identifier));
    }
}

// domain/repository/PokemonDetail.java
public record PokemonDetail(PokedexNumber number, PokemonProfile profile, EvolutionStage evolutionChain) {
    public PokemonDetail {
        Objects.requireNonNull(number, "number must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(evolutionChain, "evolutionChain must not be null");
    }
}

// domain/repository/EvolutionStage.java — a tree, because lineages branch (Eevee has 8 children)
public record EvolutionStage(String speciesName, PokedexNumber number, List<EvolutionStage> evolvesTo) {
    public EvolutionStage {
        Objects.requireNonNull(speciesName, "speciesName must not be null");
        Objects.requireNonNull(number, "number must not be null");
        evolvesTo = evolvesTo == null ? List.of() : List.copyOf(evolvesTo);
    }
}

// domain/repository/PokemonDataUnavailableException.java
/**
 * Technical failure reaching PokeAPI (timeout, 5xx, I/O). Deliberately NOT a DomainException:
 * nothing about the business was violated. It is part of the port's contract so interfaces/ can
 * map it to 503 without depending on the infrastructure adapter that throws it.
 */
public class PokemonDataUnavailableException extends RuntimeException {
    public PokemonDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## Exceptions

`DomainException` is abstract, and every exception is unchecked. There's no HTTP status and no
error code on any of them: `interfaces/rest` maps by **category**. The message is the contract
(the frontend shows it), so it should be precise and never contain a secret.

```java
// domain/exception/DomainException.java
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}

// domain/exception/NotFoundException.java — 404
public abstract class NotFoundException extends DomainException {
    protected NotFoundException(String message) { super(message); }
}

// domain/exception/ConflictException.java — 409
public abstract class ConflictException extends DomainException {
    protected ConflictException(String message) { super(message); }
}

// domain/exception/ValidationException.java — 400 (semantic validation owned by the domain)
public abstract class ValidationException extends DomainException {
    protected ValidationException(String message) { super(message); }
}

// domain/exception/UnauthenticatedException.java — 401
public abstract class UnauthenticatedException extends DomainException {
    protected UnauthenticatedException(String message) { super(message); }
}

// domain/exception/PokemonNotFoundException.java — PokeAPI has no such Pokémon
public class PokemonNotFoundException extends NotFoundException {
    public PokemonNotFoundException(PokemonIdentifier identifier) {
        super("Pokémon not found: " + identifier.value());
    }
}

// domain/exception/LocalPokemonNotFoundException.java — exists in PokeAPI, but not synced
public class LocalPokemonNotFoundException extends NotFoundException {
    public LocalPokemonNotFoundException(PokemonIdentifier identifier) {
        super("Pokémon '" + identifier.value() + "' is not in the local database");
    }
}

// domain/exception/PokemonAlreadySyncedException.java
public class PokemonAlreadySyncedException extends ConflictException {
    public PokemonAlreadySyncedException(PokedexNumber number) {
        super("Pokémon #" + number.value() + " is already in the local database");
    }
}

// domain/exception/InvalidTagException.java
public class InvalidTagException extends ValidationException {
    public InvalidTagException(String value) {
        super("Invalid tag '" + value + "': use 1-30 lowercase letters, digits or hyphens");
    }
}

// domain/exception/InvalidCredentialsException.java
public class InvalidCredentialsException extends UnauthenticatedException {
    public InvalidCredentialsException() {
        // Same message for "no such email" and "wrong password" — no account enumeration.
        super("Invalid email or password");
    }
}
```

A new `*NotFoundException` or `*ConflictException` costs no change to `GlobalExceptionHandler`,
because it inherits its status and `code` from the category.
