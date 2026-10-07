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

    private static List<BaseStat> allStats(int hp, int atk, int def, int spAtk, int spDef, int speed) { /* ... */ }
}

// test/.../fixture/LocalPokemonFixture.java
public final class LocalPokemonFixture {

    public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    public static final LocalPokemonId PIKACHU_ID =
        new LocalPokemonId(UUID.fromString("00000000-0000-0000-0000-000000000025"));
    public static final PokedexNumber PIKACHU_NUMBER = new PokedexNumber(25);

    public static LocalPokemon syncedPikachu() {
        return LocalPokemon.create(PIKACHU_ID, PIKACHU_NUMBER, NOW);
    }

    // A record whose own fields were already filled in: rebuilt as the mapper would.
    public static LocalPokemon renamedPikachu() {
        return LocalPokemon.builder()
            .id(PIKACHU_ID)
            .pokedexNumber(PIKACHU_NUMBER)
            .customAttributes(new CustomAttributes("Pica", "Kanto", Set.of(new Tag("starter"), new Tag("mascot"))))
            .syncedAt(NOW)
            .updatedAt(NOW.plusSeconds(60))
            .build();
    }
}
```

## Domain unit test

Plain JUnit 5. No Spring, no mocks. Most of the thinking happens here.

```java
// test/.../domain/model/LocalPokemonTest.java
class LocalPokemonTest {

    @Test
    void shouldBeSyncedWithItsNumberAndNoneOfOurFieldsYet() {
        var pikachu = LocalPokemon.create(PIKACHU_ID, PIKACHU_NUMBER, NOW);

        assertThat(pikachu.getPokedexNumber()).isEqualTo(PIKACHU_NUMBER);
        assertThat(pikachu.getCustomAttributes()).isEqualTo(CustomAttributes.empty());
        assertThat(pikachu.getSyncedAt()).isEqualTo(NOW);
        assertThat(pikachu.getUpdatedAt()).isEqualTo(NOW);
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

`@ExtendWith(MockitoExtension.class)`. **Mock ports** (repository, `PokemonRepository`, `UnitOfWork`)
and use **real pure collaborators** (`new PokemonMapper()`). Use BDD Mockito only. Name the
test after the interactor class, `SyncPokemonInteractorTest`.

```java
// test/.../application/usecase/SyncPokemonInteractorTest.java
@ExtendWith(MockitoExtension.class)
class SyncPokemonInteractorTest {

    @Mock private PokemonRepository pokemonRepository;
    @Mock private LocalPokemonRepository localPokemonRepository;
    @Mock private UnitOfWork unitOfWork;

    private SyncPokemonInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new SyncPokemonInteractor(pokemonRepository, localPokemonRepository,
            new PokemonMapper(), unitOfWork);   // the mapper is real — never mocked
        // Run the transactional body inline. lenient(): tests that fail before the boundary opens
        // never reach it.
        lenient().when(unitOfWork.inTransaction(ArgumentMatchers.<Supplier<Object>>any()))
            .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
    }

    @Test
    void shouldRecordThePokemonUnderItsNumberWithTheGivenIdAndTime() {
        // getByIdentifier is a DEFAULT method — stub it directly. Mockito doesn't run default
        // bodies on a mock, so stubbing findByIdentifier would leave getByIdentifier returning null.
        given(pokemonRepository.getByIdentifier(new PokemonIdentifier("pikachu"))).willReturn(PokemonFixture.pikachuDetail());
        given(localPokemonRepository.save(any(LocalPokemon.class))).willAnswer(invocation -> invocation.getArgument(0));

        var output = interactor.execute(new SyncPokemonInput("Pikachu"), PIKACHU_ID, NOW);

        assertThat(output.pokedexNumber()).isEqualTo(25);
        assertThat(output.tags()).isEmpty();
        assertThat(output.syncedAt()).isEqualTo(NOW);
        // The id was passed in, so the saved record is assertable too.
        then(localPokemonRepository).should().save(argThat(saved -> saved.getId().equals(PIKACHU_ID)));
    }

    @Test
    void shouldRefuseAPokemonThatIsAlreadySynced() {
        given(pokemonRepository.getByIdentifier(new PokemonIdentifier("pikachu"))).willReturn(PokemonFixture.pikachuDetail());
        given(localPokemonRepository.findByPokedexNumber(new PokedexNumber(25)))
            .willReturn(Optional.of(LocalPokemonFixture.syncedPikachu()));

        assertThatThrownBy(() -> interactor.execute(new SyncPokemonInput("pikachu"), PIKACHU_ID, NOW))
            .isInstanceOf(PokemonAlreadySyncedException.class);

        then(localPokemonRepository).should(never()).save(any());  // not observable from the exception
    }

    @Test
    void shouldNotOpenTransactionWhenPokeApiIsUnavailable() {
        given(pokemonRepository.getByIdentifier(any()))
            .willThrow(new PokemonDataUnavailableException("down", new IOException()));

        assertThatThrownBy(() -> interactor.execute(new SyncPokemonInput("pikachu"), PIKACHU_ID, NOW))
            .isInstanceOf(PokemonDataUnavailableException.class);

        // Guards the "no remote call inside the transaction" rule.
        then(unitOfWork).shouldHaveNoInteractions();
    }

    @Test
    void shouldRejectBlankIdentifierBeforeCallingPokeApi() {
        assertThatThrownBy(() -> interactor.execute(new SyncPokemonInput("  "), PIKACHU_ID, NOW))
            .isInstanceOf(InvalidPokemonIdentifierException.class);

        then(pokemonRepository).shouldHaveNoInteractions();
    }
}

// test/.../application/usecase/GetPokemonInteractorTest.java — the merge
@ExtendWith(MockitoExtension.class)
class GetPokemonInteractorTest {

    @Mock private PokemonRepository pokemonRepository;
    @Mock private LocalPokemonRepository localPokemonRepository;
    private GetPokemonInteractor interactor;   // set up with new PokemonMapper()

    @Test
    void shouldMergeLocalAttributesWhenPokemonIsSynced() {
        given(pokemonRepository.getByIdentifier(any())).willReturn(PokemonFixture.pikachuDetail());
        given(localPokemonRepository.findByPokedexNumber(new PokedexNumber(25)))
            .willReturn(Optional.of(LocalPokemonFixture.renamedPikachu()));

        var output = interactor.execute(new GetPokemonInput("pikachu"));

        assertThat(output.name()).isEqualTo("pikachu");
        assertThat(output.local().localizedName()).isEqualTo("Pica");
        assertThat(output.local().region()).isEqualTo("Kanto");
        assertThat(output.stats()).hasSize(6);   // from PokeAPI, not from the local record
    }

    @Test
    void shouldReturnNullLocalAndOriginalNameWhenNotSynced() {
        given(pokemonRepository.getByIdentifier(any())).willReturn(PokemonFixture.pikachuDetail());
        given(localPokemonRepository.findByPokedexNumber(any())).willReturn(Optional.empty());

        var output = interactor.execute(new GetPokemonInput("pikachu"));

        assertThat(output.local()).isNull();
        assertThat(output.name()).isEqualTo("pikachu");
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
@Import({JpaLocalPokemonRepository.class, LocalPokemonEntityMapper.class})  // slices don't scan @Components
class JpaLocalPokemonRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    // Testcontainers 2.x: org.testcontainers.postgresql.PostgreSQLContainer — verify against the resolved jar.

    @Autowired private JpaLocalPokemonRepository repository;

    @Test
    void shouldSaveAndReloadTheWholeRecordWithItsTags() {
        var pikachu = LocalPokemonFixture.renamedPikachu();

        repository.save(pikachu);

        assertThat(repository.findByPokedexNumber(PIKACHU_NUMBER)).hasValueSatisfying(reloaded -> {
            assertThat(reloaded.getId()).isEqualTo(pikachu.getId());
            assertThat(reloaded.getCustomAttributes()).isEqualTo(pikachu.getCustomAttributes()); // tags included
            assertThat(reloaded.getSyncedAt()).isEqualTo(pikachu.getSyncedAt());
            assertThat(reloaded.getUpdatedAt()).isEqualTo(pikachu.getUpdatedAt());
        });
    }

    // Two syncs of #25 at the same time both pass the use case's check; the unique number decides.
    @Test
    void shouldTranslateASecondRecordOfTheSamePokemonIntoAConflict() {
        repository.save(LocalPokemonFixture.syncedPikachu());
        var secondPikachu = LocalPokemon.create(
            new LocalPokemonId(UUID.fromString("00000000-0000-0000-0000-000000000099")), PIKACHU_NUMBER, NOW);

        assertThatThrownBy(() -> repository.save(secondPikachu))
            .isInstanceOf(PokemonAlreadySyncedException.class)
            .hasMessage("Pokémon #25 is already in the local database");
    }
}
```

`hasValueSatisfying` instead of `.get()`: `Optional.get()` is forbidden even in tests.

The real migrations include the demo seed (D-005), so these tests run on a database that already
has rows. The fixture uses Pikachu, which the seed deliberately leaves out. Tests assert on their
own rows, never on absolute counts.

## PokeAPI adapter tests

**Translation**: plain JUnit against recorded JSON (`src/test/resources/pokeapi/pokemon-25.json`,
`pokemon-species-25.json`, `evolution-chain-10.json`, plus an Eevee chain for branching). Record
them once with `curl` and commit them. Tests never hit the network. Pokémon responses are trimmed
of `moves`, `game_indices` and `sprites.versions` (unused, ~95% of the size); everything else stays
as served. `PokeApiFixtures` (test source set) reads them with Jackson. When a rule depends on
order (slots), the test reverses the real array, because PokeAPI happens to send it sorted.

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
`Optional.empty()`, 500 → `PokemonDataUnavailableException`, and a timeout →
`PokemonDataUnavailableException`.

**Caching**: a small Spring test with the cache enabled, `PokeApiClient`, `PokeApiPokemonRepository`
and a `MockRestServiceServer`. Call **`PokemonRepository.getByIdentifier`** (the default method the
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
         JwtConfig.class, ErrorResponseAuthenticationEntryPoint.class,
         PokemonControllerIT.FixedClock.class})
class PokemonControllerIT {

    @TestConfiguration
    static class FixedClock {
        @Bean Clock clock() { return Clock.fixed(LocalPokemonFixture.NOW, ZoneOffset.UTC); }
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
                Optional.of(LocalPokemonFixture.renamedPikachu())));

        mockMvc.perform(get("/api/v1/pokemon/pikachu"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("pikachu"))
            .andExpect(jsonPath("$.local.localizedName").value("Pica"))
            .andExpect(jsonPath("$.local.region").value("Kanto"));
    }

    @Test
    void shouldReturn201WithLocationWhenPokemonIsSynced() throws Exception {
        given(syncPokemonUseCase.execute(any(), any(), eq(LocalPokemonFixture.NOW)))
            .willReturn(LocalPokemonOutput.from(LocalPokemonFixture.syncedPikachu()));

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
    static final ArchRule transactional_classes_only_in_the_unit_of_work_adapter =
        noClasses().that().resideOutsideOfPackage("..infrastructure.transaction..")
            .should().beAnnotatedWith(Transactional.class);

    // ArchUnit 1.5 has no "classes that contain methods annotated with…" condition, so methods get
    // their own rule. The that() selects the methods outside the adapter: selecting the annotated
    // ones would match nothing in a correct codebase, and ArchUnit fails a rule that checks nothing.
    @ArchTest
    static final ArchRule transactional_methods_only_in_the_unit_of_work_adapter =
        noMethods().that().areDeclaredInClassesThat().resideOutsideOfPackage("..infrastructure.transaction..")
            .should().beAnnotatedWith(Transactional.class);

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

Gradle: `testImplementation 'com.tngtech.archunit:archunit-junit5:1.5.1'`. A rule whose `that()`
matches nothing fails (ArchUnit's default, `failOnEmptyShould`), so a typo in a package name can't
make a rule silently pass. Write "no X" rules so the `that()` selects what's being checked, not the
violation. The class is named
`*Test`, not `*IT`, on purpose, so it runs on every `./gradlew test`.
