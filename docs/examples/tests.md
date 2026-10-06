# Tests — Examples

Reference tests for every layer, against the real model in
[`../domain-model.md`](../domain-model.md). Rules and the per-layer tool table:
[`../standards/backend.md#testing`](../standards/backend.md#testing).

TDD order for any feature: **domain → interactor → adapters**. Each cycle goes red (for the right
reason), then green, then refactor.

## Fixtures (Object Mother)

One per aggregate (plus one for PokeAPI data), in the test source set, with a fixed `NOW`. Fixtures
never read the clock, so a test can't fail at midnight.

```java
// test/.../fixture/PokemonFixture.java
public final class PokemonFixture {

    public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    public static final LocalPokemonId PIKACHU_ID =
        new LocalPokemonId(UUID.fromString("00000000-0000-0000-0000-000000000025"));

    private PokemonFixture() {}

    public static PokemonProfile pikachuProfile() {
        return new PokemonProfile(
            "pikachu", "Mouse Pokémon",
            Height.fromDecimetres(4), Weight.fromHectograms(60),
            "https://example.test/sprites/25.png", "https://example.test/artwork/25.png",
            List.of(new PokemonType("electric")),
            List.of(new Ability("static", false), new Ability("lightning-rod", true)),
            allStats(35, 55, 40, 50, 50, 90),
            "When several of these Pokémon gather, their electricity could build and cause lightning storms.");
    }

    /** What PokeAPI returns for Pikachu, as the domain sees it. */
    public static PokemonDetail pikachuDetail() {
        var raichu = new EvolutionStage("raichu", new PokedexNumber(26), List.of());
        var pikachu = new EvolutionStage("pikachu", new PokedexNumber(25), List.of(raichu));
        var pichu = new EvolutionStage("pichu", new PokedexNumber(172), List.of(pikachu));
        return new PokemonDetail(new PokedexNumber(25), pikachuProfile(), pichu);
    }

    /** Pikachu right after a sync: no custom attributes yet. */
    public static LocalPokemon localPikachu() {
        return LocalPokemon.create(PIKACHU_ID, new PokedexNumber(25), pikachuProfile().toSnapshot(), NOW);
    }

    public static LocalPokemon localPikachuWithCustomAttributes() {
        var pokemon = localPikachu();
        pokemon.updateCustomAttributes(
            new CustomAttributes("Pikachu BR", "Kanto", Set.of(new Tag("mascot"))), NOW);
        return pokemon;
    }

    private static List<BaseStat> allStats(int hp, int atk, int def, int spAtk, int spDef, int speed) { /* ... */ }
}
```

## Domain unit test

Plain JUnit 5. No Spring, no mocks. Most of the thinking happens here.

```java
// test/.../domain/model/LocalPokemonTest.java
class LocalPokemonTest {

    @Test
    void shouldStartWithEmptyCustomAttributesWhenCreated() {
        var pokemon = PokemonFixture.localPikachu();

        assertThat(pokemon.getCustomAttributes()).isEqualTo(CustomAttributes.empty());
        assertThat(pokemon.getSyncedAt()).isEqualTo(PokemonFixture.NOW);
        assertThat(pokemon.getUpdatedAt()).isEqualTo(PokemonFixture.NOW);
    }

    @Test
    void shouldReplaceCustomAttributesWithoutTouchingSnapshotWhenUpdated() {
        var pokemon = PokemonFixture.localPikachu();
        var later = PokemonFixture.NOW.plusSeconds(60);
        var attributes = new CustomAttributes("Pikachu BR", "Kanto", Set.of(new Tag("starter")));

        pokemon.updateCustomAttributes(attributes, later);

        assertThat(pokemon.getCustomAttributes()).isEqualTo(attributes);
        assertThat(pokemon.getSnapshot()).isEqualTo(PokemonFixture.pikachuProfile().toSnapshot());
        assertThat(pokemon.getUpdatedAt()).isEqualTo(later);
        assertThat(pokemon.getSyncedAt()).isEqualTo(PokemonFixture.NOW);
    }

    @Test
    void shouldUseLocalizedNameAsDisplayNameWhenSet() {
        assertThat(PokemonFixture.localPikachuWithCustomAttributes().displayName()).isEqualTo("Pikachu BR");
    }

    @Test
    void shouldFallBackToOriginalNameWhenLocalizedNameIsNotSet() {
        assertThat(PokemonFixture.localPikachu().displayName()).isEqualTo("pikachu");
    }
}

// test/.../domain/model/TagTest.java — VO edge cases as a parameterized table
class TagTest {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "-starter", "Has Space", "way-too-long-tag-name-over-thirty-chars", "ção"})
    void shouldRejectInvalidTag(String raw) {
        assertThatThrownBy(() -> new Tag(raw)).isInstanceOf(InvalidTagException.class);
    }

    @Test
    void shouldNormalizeCaseAndWhitespaceWhenValid() {
        assertThat(new Tag("  Starter ").value()).isEqualTo("starter");
    }
}
```

## Interactor unit test

`@ExtendWith(MockitoExtension.class)`. **Mock ports** (repository, `PokemonSource`, `UnitOfWork`)
and use **real pure collaborators** (`new LocalPokemonMapper()`). Use BDD Mockito only. Name the
test after the interactor class, `SyncPokemonInteractorTest`.

```java
// test/.../application/usecase/SyncPokemonInteractorTest.java
@ExtendWith(MockitoExtension.class)
class SyncPokemonInteractorTest {

    @Mock private PokemonSource source;
    @Mock private LocalPokemonRepository repository;
    @Mock private UnitOfWork unitOfWork;

    private final LocalPokemonMapper mapper = new LocalPokemonMapper();   // real — never mocked

    private SyncPokemonInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new SyncPokemonInteractor(source, repository, mapper, unitOfWork);
        // Run the transactional body inline. lenient(): tests that fail before the boundary opens
        // never reach it.
        lenient().when(unitOfWork.inTransaction(ArgumentMatchers.<Supplier<Object>>any()))
            .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
    }

    @Test
    void shouldSaveSnapshotWithGivenIdWhenNotSyncedYet() {
        // getByIdentifier is a DEFAULT method — stub it directly. Mockito doesn't run default
        // bodies on a mock, so stubbing findByIdentifier would leave getByIdentifier returning null.
        given(source.getByIdentifier(new PokemonIdentifier("pikachu"))).willReturn(PokemonFixture.pikachuDetail());
        given(repository.findByPokedexNumber(new PokedexNumber(25))).willReturn(Optional.empty());
        given(repository.save(any(LocalPokemon.class))).willAnswer(i -> i.getArgument(0));

        var output = interactor.execute(new SyncPokemonInput("Pikachu"), PokemonFixture.PIKACHU_ID, PokemonFixture.NOW);

        assertThat(output.pokedexNumber()).isEqualTo(25);
        assertThat(output.name()).isEqualTo("pikachu");
        assertThat(output.displayName()).isEqualTo("pikachu");   // no localized name yet
        assertThat(output.tags()).isEmpty();
        assertThat(output.syncedAt()).isEqualTo(PokemonFixture.NOW);
        // The id was passed in, so the saved record is assertable too.
        then(repository).should().save(argThat(saved -> saved.getId().equals(PokemonFixture.PIKACHU_ID)));
    }

    @Test
    void shouldThrowConflictWhenPokemonIsAlreadySynced() {
        given(source.getByIdentifier(any())).willReturn(PokemonFixture.pikachuDetail());
        given(repository.findByPokedexNumber(new PokedexNumber(25))).willReturn(Optional.of(PokemonFixture.localPikachu()));

        assertThatThrownBy(() -> interactor.execute(new SyncPokemonInput("pikachu"), PokemonFixture.PIKACHU_ID, PokemonFixture.NOW))
            .isInstanceOf(PokemonAlreadySyncedException.class);

        then(repository).should(never()).save(any());  // not observable from the exception
    }

    @Test
    void shouldNotOpenTransactionWhenPokeApiIsUnavailable() {
        given(source.getByIdentifier(any()))
            .willThrow(new PokemonSourceUnavailableException("down", new IOException()));

        assertThatThrownBy(() -> interactor.execute(new SyncPokemonInput("pikachu"), PokemonFixture.PIKACHU_ID, PokemonFixture.NOW))
            .isInstanceOf(PokemonSourceUnavailableException.class);

        // Guards the "no remote call inside the transaction" rule.
        then(unitOfWork).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectBlankIdentifierBeforeCallingPokeApi() {
        assertThatThrownBy(() -> interactor.execute(new SyncPokemonInput("  "), PokemonFixture.PIKACHU_ID, PokemonFixture.NOW))
            .isInstanceOf(InvalidPokemonIdentifierException.class);

        then(source).shouldHaveNoInteractions();
    }
}

// test/.../application/usecase/GetPokemonInteractorTest.java — the merge
@ExtendWith(MockitoExtension.class)
class GetPokemonInteractorTest {

    @Mock private PokemonSource source;
    @Mock private LocalPokemonRepository repository;
    private GetPokemonInteractor interactor;   // set up with new LocalPokemonMapper()

    @Test
    void shouldMergeLocalAttributesWhenPokemonIsSynced() {
        given(source.getByIdentifier(any())).willReturn(PokemonFixture.pikachuDetail());
        given(repository.findByPokedexNumber(new PokedexNumber(25)))
            .willReturn(Optional.of(PokemonFixture.localPikachuWithCustomAttributes()));

        var output = interactor.execute(new GetPokemonInput("pikachu"));

        assertThat(output.name()).isEqualTo("pikachu");
        assertThat(output.displayName()).isEqualTo("Pikachu BR");
        assertThat(output.local().region()).isEqualTo("Kanto");
        assertThat(output.stats()).hasSize(6);   // from PokeAPI, not from the local record
    }

    @Test
    void shouldReturnNullLocalAndOriginalNameWhenNotSynced() {
        given(source.getByIdentifier(any())).willReturn(PokemonFixture.pikachuDetail());
        given(repository.findByPokedexNumber(any())).willReturn(Optional.empty());

        var output = interactor.execute(new GetPokemonInput("pikachu"));

        assertThat(output.local()).isNull();
        assertThat(output.displayName()).isEqualTo("pikachu");
    }
}
```

For a `void` interactor (`RemoveLocalPokemonInteractor`), stub the **`Runnable`** overload:

```java
willAnswer(invocation -> { invocation.<Runnable>getArgument(0).run(); return null; })
    .given(unitOfWork).inTransaction(any(Runnable.class));
```

## Repository integration test

`@DataJpaTest` + Testcontainers **PostgreSQL** (the same engine as production) + the real Flyway
migrations. It tests the adapter, not Spring Data. Assert the **whole aggregate** after a round
trip.

```java
// test/.../infrastructure/persistence/repository/JpaLocalPokemonRepositoryIT.java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaLocalPokemonRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    // Testcontainers 2.x: org.testcontainers.postgresql.PostgreSQLContainer — verify against the resolved jar.

    @Autowired private LocalPokemonJpaRepository jpaRepository;

    // Constructed: @DataJpaTest doesn't scan plain @Components.
    private final LocalPokemonEntityMapper mapper = new LocalPokemonEntityMapper();
    private JpaLocalPokemonRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaLocalPokemonRepository(jpaRepository, mapper);
    }

    @Test
    void shouldPersistAndReloadWholeAggregate() {
        var pokemon = PokemonFixture.localPikachuWithCustomAttributes();

        repository.save(pokemon);

        assertThat(repository.findByPokedexNumber(new PokedexNumber(25))).hasValueSatisfying(reloaded -> {
            assertThat(reloaded.getSnapshot()).isEqualTo(pokemon.getSnapshot());
            assertThat(reloaded.getCustomAttributes()).isEqualTo(pokemon.getCustomAttributes()); // tags included
            assertThat(reloaded.getSyncedAt()).isEqualTo(pokemon.getSyncedAt());
        });
    }

    @Test
    void shouldFindByNameOrNumber() {
        repository.save(PokemonFixture.localPikachu());

        assertThat(repository.findByIdentifier(new PokemonIdentifier("pikachu"))).isPresent();
        assertThat(repository.findByIdentifier(new PokemonIdentifier("25"))).isPresent();
    }

    @Test
    void shouldTranslateDuplicatePokedexNumberIntoConflict() {
        repository.save(PokemonFixture.localPikachu());
        var duplicate = LocalPokemon.create(new LocalPokemonId(UUID.fromString("00000000-0000-0000-0000-000000000099")),
            new PokedexNumber(25), PokemonFixture.pikachuProfile().toSnapshot(), PokemonFixture.NOW);

        assertThatThrownBy(() -> repository.save(duplicate)).isInstanceOf(PokemonAlreadySyncedException.class);
    }

    @Test
    void shouldReturnOnlyRequestedNumbersInOneQuery() { /* findAllByPokedexNumbers; empty input → empty list */ }
}
```

`hasValueSatisfying` instead of `.get()`: `Optional.get()` is forbidden even in tests.

The real migrations include the demo seed (D-005), so these tests run on a database that already
has rows. The fixture uses Pikachu, which the seed deliberately leaves out. Tests assert on their
own rows, never on absolute counts.

## PokeAPI adapter tests

**Translation**: plain JUnit against recorded JSON (`src/test/resources/pokeapi/pokemon-25.json`,
`pokemon-species-25.json`, `evolution-chain-10.json`, plus an Eevee chain for branching). Record
them once with `curl` and commit them. Tests never hit the network.

```java
// test/.../infrastructure/external/pokeapi/PokeApiTranslatorTest.java
class PokeApiTranslatorTest {

    private final PokeApiTranslator translator = new PokeApiTranslator();

    @Test
    void shouldConvertHectogramsAndDecimetresToKilogramsAndMeters() { /* weight 60 → 6.0 kg, height 4 → 0.4 m */ }

    @Test
    void shouldPickEnglishGenusAsCategory() { /* "Mouse Pokémon" */ }

    @Test
    void shouldNormalizeFormFeedsAndNewlinesInFlavorText() { /* "\f" and "\n" → single spaces */ }

    @Test
    void shouldBuildBranchingEvolutionTreeWhenChainBranches() { /* eevee → 8 children */ }

    @Test
    void shouldAcceptMissingArtworkWhenPokeApiHasNone() { /* null artwork → null, no NPE */ }
}
```

**HTTP behaviour**: `@RestClientTest(PokeApiClient.class)` + `MockRestServiceServer`. Cover 404 →
`Optional.empty()`, 500 → `PokemonSourceUnavailableException`, and a timeout →
`PokemonSourceUnavailableException`.

**Caching**: a small Spring test with the cache enabled, `PokeApiClient`, `PokeApiPokemonSource`
and a `MockRestServiceServer`. Call **`PokemonSource.getByIdentifier`** (the default method the
interactors use) twice and assert the second call makes no HTTP request. Going through the default
method is the point: it's the path where a `@Cacheable` on the adapter itself would be silently
bypassed (self-invocation, see `standards/backend.md`).

## Controller integration test

`@WebMvcTest` + `@MockitoBean` on the **input-port interfaces** + `@Import` of the pure web
collaborators (REST mapper, fixed `Clock`, security config and its `ErrorResponse` writers). Cover
every status in the contract, and both the public reads and the protected writes.

```java
// test/.../interfaces/rest/controller/PokemonControllerIT.java
@WebMvcTest(PokemonController.class)
@Import({PokemonRestMapper.class, SecurityConfig.class,
         ErrorResponseAuthenticationEntryPoint.class, ErrorResponseAccessDeniedHandler.class,
         PokemonControllerIT.FixedClock.class})
class PokemonControllerIT {

    @TestConfiguration
    static class FixedClock {
        @Bean Clock clock() { return Clock.fixed(PokemonFixture.NOW, ZoneOffset.UTC); }
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private BrowsePokemonUseCase browsePokemonUseCase;          // interfaces, never *Interactor
    @MockitoBean private GetPokemonUseCase getPokemonUseCase;
    @MockitoBean private GetLocalPokemonUseCase getLocalPokemonUseCase;
    @MockitoBean private SyncPokemonUseCase syncPokemonUseCase;
    @MockitoBean private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;
    @MockitoBean private RemoveLocalPokemonUseCase removeLocalPokemonUseCase;

    @Test
    void shouldReturnMergedDetailWithoutTokenBecauseReadsArePublic() throws Exception {
        given(getPokemonUseCase.execute(any())).willReturn(
            PokemonDetailOutput.from(PokemonFixture.pikachuDetail(),
                Optional.of(PokemonFixture.localPikachuWithCustomAttributes())));

        mockMvc.perform(get("/api/v1/pokemon/pikachu"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("pikachu"))
            .andExpect(jsonPath("$.displayName").value("Pikachu BR"))
            .andExpect(jsonPath("$.local.region").value("Kanto"));
    }

    @Test
    void shouldReturn201WithLocationWhenPokemonIsSynced() throws Exception {
        given(syncPokemonUseCase.execute(any(), any(), eq(PokemonFixture.NOW)))
            .willReturn(LocalPokemonOutput.from(PokemonFixture.localPikachu()));

        mockMvc.perform(post("/api/v1/pokemon/pikachu/local").with(jwt()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/v1/pokemon/25/local")))
            .andExpect(jsonPath("$.pokedexNumber").value(25));
    }

    @Test
    void shouldReturn401WhenSyncingWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/pokemon/pikachu/local"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldReturn400WhenBodyIsMalformedJson() throws Exception {
        mockMvc.perform(put("/api/v1/pokemon/pikachu/local").with(jwt())
                .contentType(MediaType.APPLICATION_JSON).content("{ \"tags\": [ "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn400WithFieldErrorsWhenTooManyTags() throws Exception { /* 11 tags → fieldErrors[0].field == "tags" */ }

    @Test
    void shouldReturn404WhenUpdatingAPokemonThatIsNotSynced() throws Exception {
        willThrow(new LocalPokemonNotFoundException(new PokemonIdentifier("pikachu")))
            .given(updateLocalPokemonUseCase).execute(any(), any());
        // ... PUT → 404, $.code == NOT_FOUND, $.message == "Pokémon 'pikachu' is not in the local database"
    }
}
```

Boot 4 ships **Jackson 3**. Inline JSON text blocks avoid depending on `ObjectMapper` at all. If you
do need one, it's `tools.jackson.databind.json.JsonMapper`.

## Exception handler integration test

This proves the category property: an exception the handler has **never heard of** still gets the
right status and code. It also asserts the message, because the message is the contract.

```java
@Test
void shouldMapAnyConflictCategoryExceptionTo409() throws Exception {
    willThrow(new LocalPokemonModifiedConcurrentlyException(new PokedexNumber(25)))
        .given(updateLocalPokemonUseCase).execute(any(), any());
    // PUT → 409, $.code == "CONFLICT"
}
```

## Composition-root test

```java
// test/.../ApplicationContextIT.java — @SpringBootTest + Testcontainers Postgres, PokeAPI never called
@Test
void shouldExposeEveryInputPortAsABean(ApplicationContext context) {
    // Scan application.usecase for interfaces ending in "UseCase" and assert
    // context.getBean(type) resolves for each — a forgotten @Bean in UseCaseConfig fails HERE.
}
```

## Architecture test

ArchUnit, with no Spring and no DB. It runs in the normal `test` task, so a layering violation
fails every build. Rules are `static final` fields in `snake_case`, because the field name is the
headline of the failure message.

```java
// test/.../architecture/LayeredArchitectureTest.java
@AnalyzeClasses(packages = "dev.guilhermeds.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String INFRASTRUCTURE = "..infrastructure..";
    private static final String INTERFACES = "..interfaces..";

    // ---- the two framework-free layers (allowlists, never denylists) ----------

    @ArchTest
    static final ArchRule domain_depends_only_on_java_and_itself =
        classes().that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN);

    // Strict Clean Architecture: no @Service/@Component/@Transactional/slf4j in use cases.
    @ArchTest
    static final ArchRule application_depends_only_on_java_domain_and_itself =
        classes().that().resideInAPackage(APPLICATION)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN, APPLICATION);

    // ---- adapters don't know each other ---------------------------------------

    @ArchTest
    static final ArchRule interfaces_must_not_depend_on_infrastructure =
        noClasses().that().resideInAPackage(INTERFACES)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE);

    // SecurityConfig gets the ErrorResponse writers through Spring Security's interfaces.
    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_interfaces =
        noClasses().that().resideInAPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage(INTERFACES);

    // ---- input ports ------------------------------------------------------------

    @ArchTest
    static final ArchRule interfaces_depend_on_input_ports_not_interactors =
        noClasses().that().resideInAPackage(INTERFACES)
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Interactor");

    @ArchTest
    static final ArchRule interactors_live_in_usecase_and_implement_an_input_port =
        classes().that().haveSimpleNameEndingWith("Interactor")
            .should().resideInAPackage("..application.usecase..")
            .andShould().implement(JavaClass.Predicates.simpleNameEndingWith("UseCase"));

    @ArchTest
    static final ArchRule input_ports_are_interfaces =
        classes().that().resideInAPackage("..application.usecase..")
            .and().haveSimpleNameEndingWith("UseCase")
            .should().beInterfaces();

    // ---- persistence ------------------------------------------------------------

    @ArchTest
    static final ArchRule jpa_entities_live_only_in_persistence_entity =
        classes().that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.persistence.entity..");

    @ArchTest
    static final ArchRule no_cross_aggregate_jpa_associations =
        noFields().should().beAnnotatedWith(ManyToOne.class)
            .orShould().beAnnotatedWith(OneToOne.class)
            .orShould().beAnnotatedWith(ManyToMany.class);

    @ArchTest
    static final ArchRule transactional_only_in_the_unit_of_work_adapter =
        noClasses().that().resideOutsideOfPackage("..infrastructure.transaction..")
            .should().beAnnotatedWith(Transactional.class)
            .orShould().containAnyMethodsThat(annotatedWith(Transactional.class));

    // ---- determinism --------------------------------------------------------------

    @ArchTest
    static final ArchRule clock_read_only_at_the_edge =
        noClasses().that().resideOutsideOfPackage(INTERFACES)
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(LocalDate.class, "now");

    @ArchTest
    static final ArchRule random_uuid_only_inside_domain_model =
        noClasses().that().resideOutsideOfPackage("..domain.model..")
            .should().callMethod(UUID.class, "randomUUID");

    // ---- hygiene --------------------------------------------------------------------

    @ArchTest
    static final ArchRule no_optional_get = noClasses().should().callMethod(Optional.class, "get");

    @ArchTest
    static final ArchRule no_field_injection = noFields().should().beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule no_setters_in_domain =
        noMethods().that().haveNameStartingWith("set")
            .should().beDeclaredInClassesThat().resideInAPackage(DOMAIN);

    private static final DescribedPredicate<JavaClass> ARE_RECORDS =
        new DescribedPredicate<>("are records") {
            @Override public boolean test(JavaClass c) { return c.isRecord(); }
        };

    @ArchTest
    static final ArchRule domain_entities_have_only_private_constructors =
        classes().that().resideInAPackage("..domain.model..").and(ARE_RECORDS.negate())
            .should().haveOnlyPrivateConstructors();

    // Declare static helper fields ABOVE the rules that use them (illegal forward reference otherwise).
}
```

Every rule here uses ArchUnit's fluent API: no custom `ArchCondition` to write or debug. Three
stronger rules that need one are parked (`plan.md`): generic return signatures leaking domain
model types, `@Version` on every `@Entity`, and `{Id}.generate()` called only from `interfaces`.
Until then those three are enforced by review and by the ITs (the optimistic-lock IT fails
without `@Version`).

`Instant.now(clock)` (the one-arg overload) is allowed because the rule targets the no-arg call.
`JwtTokenIssuer` doesn't trip the clock rule, because it receives `now`.

Gradle: `testImplementation 'com.tngtech.archunit:archunit-junit5:<latest 1.x>'`. The class is named
`*Test`, not `*IT`, on purpose, so it runs on every `./gradlew test`.
