package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.application.dto.AbilityOutput;
import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.application.dto.EvolutionStageOutput;
import dev.guilhermeds.backend.application.dto.GetLocalPokemonInput;
import dev.guilhermeds.backend.application.dto.GetPokemonInput;
import dev.guilhermeds.backend.application.dto.LocalAttributesOutput;
import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;
import dev.guilhermeds.backend.application.dto.PageOutput;
import dev.guilhermeds.backend.application.dto.PokemonDetailOutput;
import dev.guilhermeds.backend.application.dto.PokemonSummaryOutput;
import dev.guilhermeds.backend.application.dto.RemoveLocalPokemonInput;
import dev.guilhermeds.backend.application.dto.StatOutput;
import dev.guilhermeds.backend.application.dto.SyncPokemonInput;
import dev.guilhermeds.backend.application.dto.UpdateLocalPokemonInput;
import dev.guilhermeds.backend.application.usecase.BrowsePokemonUseCase;
import dev.guilhermeds.backend.application.usecase.GetLocalPokemonUseCase;
import dev.guilhermeds.backend.application.usecase.GetPokemonUseCase;
import dev.guilhermeds.backend.application.usecase.RemoveLocalPokemonUseCase;
import dev.guilhermeds.backend.application.usecase.SyncPokemonUseCase;
import dev.guilhermeds.backend.application.usecase.UpdateLocalPokemonUseCase;
import dev.guilhermeds.backend.domain.exception.InvalidPageRequestException;
import dev.guilhermeds.backend.domain.exception.InvalidPokedexNumberException;
import dev.guilhermeds.backend.domain.exception.InvalidPokemonIdentifierException;
import dev.guilhermeds.backend.domain.exception.InvalidTagException;
import dev.guilhermeds.backend.domain.exception.LocalPokemonDataUnavailableException;
import dev.guilhermeds.backend.domain.exception.LocalPokemonModifiedConcurrentlyException;
import dev.guilhermeds.backend.domain.exception.LocalPokemonNotFoundException;
import dev.guilhermeds.backend.domain.exception.PokemonAlreadySyncedException;
import dev.guilhermeds.backend.domain.exception.PokemonDataUnavailableException;
import dev.guilhermeds.backend.domain.exception.PokemonNotFoundException;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.model.Tag;
import dev.guilhermeds.backend.infrastructure.config.JwtConfig;
import dev.guilhermeds.backend.infrastructure.config.SecurityConfig;
import dev.guilhermeds.backend.interfaces.rest.mapper.PokemonRestMapper;
import dev.guilhermeds.backend.interfaces.rest.security.ErrorResponseAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(PokemonController.class)
@Import({PokemonRestMapper.class, SecurityConfig.class, JwtConfig.class, ErrorResponseAuthenticationEntryPoint.class,
    PokemonControllerIT.FixedClock.class})
class PokemonControllerIT {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    private static final PokemonSummaryOutput PIKACHU = new PokemonSummaryOutput(25, "pikachu", "https://img/25.png",
        "Mouse Pokémon", new BigDecimal("6.0"), List.of("electric"),
        List.of(new AbilityOutput("static", false), new AbilityOutput("lightning-rod", true)));

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private BrowsePokemonUseCase browsePokemonUseCase;

    @MockitoBean
    private GetPokemonUseCase getPokemonUseCase;

    @MockitoBean
    private SyncPokemonUseCase syncPokemonUseCase;

    @MockitoBean
    private GetLocalPokemonUseCase getLocalPokemonUseCase;

    @MockitoBean
    private UpdateLocalPokemonUseCase updateLocalPokemonUseCase;

    @MockitoBean
    private RemoveLocalPokemonUseCase removeLocalPokemonUseCase;

    @Test
    void shouldReturnAPageOfCardsToAnyone() {
        given(browsePokemonUseCase.execute(new BrowsePokemonInput(1, 20)))
            .willReturn(new PageOutput<>(List.of(PIKACHU), 1, 20, 1351));

        assertThat(mockMvc.get().uri("/api/v1/pokemon?page=1&size=20"))
            .hasStatusOk()
            .bodyJson()
            .isStrictlyEqualTo("""
                {
                  "content": [ {
                    "pokedexNumber": 25, "name": "pikachu", "spriteUrl": "https://img/25.png",
                    "category": "Mouse Pokémon", "weightKilograms": 6.0, "types": [ "electric" ],
                    "abilities": [ { "name": "static", "hidden": false }, { "name": "lightning-rod", "hidden": true } ]
                  } ],
                  "page": 1, "size": 20, "totalElements": 1351, "totalPages": 68
                }
                """);
    }

    @Test
    void shouldStartAtTheFirstPageOfTwentyWhenNothingIsAsked() {
        given(browsePokemonUseCase.execute(new BrowsePokemonInput(0, 20)))
            .willReturn(new PageOutput<>(List.of(PIKACHU), 0, 20, 1351));

        assertThat(mockMvc.get().uri("/api/v1/pokemon"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.page").isEqualTo(0);
    }

    @Test
    void shouldAnswerAPageSizeOutOfRangeWith400() {
        given(browsePokemonUseCase.execute(new BrowsePokemonInput(0, 51)))
            .willThrow(InvalidPageRequestException.sizeOutOfRange(1, 50));

        assertThat(mockMvc.get().uri("/api/v1/pokemon?size=51"))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "VALIDATION_ERROR", "message": "Size must be between 1 and 50" }
                """);
    }

    @Test
    void shouldAnswerAPageThatIsNotANumberWith400() {
        assertThat(mockMvc.get().uri("/api/v1/pokemon?page=abc"))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "VALIDATION_ERROR", "message": "Invalid request parameter" }
                """);
    }

    // The exception's message is for the log: it can name internal details, so the client gets a fixed one.
    @Test
    void shouldAnswer503WhenPokeApiIsUnavailable() {
        given(browsePokemonUseCase.execute(new BrowsePokemonInput(0, 20)))
            .willThrow(new PokemonDataUnavailableException("PokeAPI listed a Pokémon it can't return: missingno"));

        var result = mockMvc.get().uri("/api/v1/pokemon").exchange();

        assertThat(result)
            .hasStatus(503)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "DATA_UNAVAILABLE",
                  "message": "The service is temporarily unavailable. Please try again in a moment." }
                """);
        assertThat(result).bodyText().doesNotContain("missingno");
    }

    // ---- one Pokémon ----------------------------------------------------------------------------

    @Test
    void shouldDescribeOnePokemonToAnyone() {
        given(getPokemonUseCase.execute(new GetPokemonInput("pikachu"))).willReturn(new PokemonDetailOutput(25, "pikachu", "pikachu",
            "Mouse Pokémon", new BigDecimal("0.4"), new BigDecimal("6.0"), "https://img/25.png", "https://img/25-art.png",
            List.of("electric"), List.of(new AbilityOutput("static", false)), List.of(new StatOutput("HP", 35)),
            "It keeps its tail raised.", new EvolutionStageOutput("pichu", 172,
                List.of(new EvolutionStageOutput("pikachu", 25, List.of()))), null));

        assertThat(mockMvc.get().uri("/api/v1/pokemon/pikachu"))
            .hasStatusOk()
            .bodyJson()
            .isStrictlyEqualTo("""
                {
                  "pokedexNumber": 25, "name": "pikachu", "category": "Mouse Pokémon",
                  "heightMeters": 0.4, "weightKilograms": 6.0,
                  "spriteUrl": "https://img/25.png", "artworkUrl": "https://img/25-art.png",
                  "types": [ "electric" ], "abilities": [ { "name": "static", "hidden": false } ],
                  "stats": [ { "name": "HP", "value": 35 } ],
                  "description": "It keeps its tail raised.",
                  "evolutionChain": { "speciesName": "pichu", "pokedexNumber": 172,
                    "evolvesTo": [ { "speciesName": "pikachu", "pokedexNumber": 25, "evolvesTo": [] } ] },
                  "local": null
                }
                """);
    }

    @Test
    void shouldAnswerAnUnknownPokemonWith404() {
        given(getPokemonUseCase.execute(new GetPokemonInput("missingno")))
            .willThrow(new PokemonNotFoundException(new PokemonIdentifier("missingno")));

        assertThat(mockMvc.get().uri("/api/v1/pokemon/missingno"))
            .hasStatus(404)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "NOT_FOUND", "message": "Pokémon 'missingno' was not found" }
                """);
    }

    @Test
    void shouldAnswerAMalformedIdentifierWith400() {
        given(getPokemonUseCase.execute(new GetPokemonInput("pika!"))).willThrow(new InvalidPokemonIdentifierException());

        assertThat(mockMvc.get().uri("/api/v1/pokemon/pika!"))
            .hasStatus(400)
            .bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void shouldAnswer503WhenPokeApiIsUnavailableForADetail() {
        given(getPokemonUseCase.execute(new GetPokemonInput("pikachu")))
            .willThrow(new PokemonDataUnavailableException("PokeAPI is unavailable right now"));

        assertThat(mockMvc.get().uri("/api/v1/pokemon/pikachu"))
            .hasStatus(503)
            .bodyJson().extractingPath("$.code").isEqualTo("DATA_UNAVAILABLE");
    }

    // ---- the local record (US-03) -----------------------------------------------------------------

    @Test
    void shouldSyncAPokemonForASignedInUser() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("25")), any(), eq(NOW)))
            .willReturn(new LocalPokemonOutput(25, null, null, List.of(), NOW, NOW));

        assertThat(mockMvc.post().uri("/api/v1/pokemon/25/local").with(jwt()))
            .hasStatus(201)
            .hasHeader("Location", "http://localhost/api/v1/pokemon/25/local")
            .bodyJson()
            .isStrictlyEqualTo("""
                { "pokedexNumber": 25, "localizedName": null, "region": null, "tags": [],
                  "syncedAt": "2026-01-15T10:00:00Z", "updatedAt": "2026-01-15T10:00:00Z" }
                """);
    }

    @Test
    void shouldRequireATokenToSync() {
        assertThat(mockMvc.post().uri("/api/v1/pokemon/25/local"))
            .hasStatus(401)
            .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
        verifyNoInteractions(syncPokemonUseCase);
    }

    @Test
    void shouldAnswerASyncOfAnUnknownPokemonWith404() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("99999")), any(), any()))
            .willThrow(new PokemonNotFoundException(new PokemonIdentifier("99999")));

        assertThat(mockMvc.post().uri("/api/v1/pokemon/99999/local").with(jwt())).hasStatus(404);
    }

    @Test
    void shouldAnswerASecondSyncWith409() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("25")), any(), any()))
            .willThrow(new PokemonAlreadySyncedException(new PokedexNumber(25)));

        assertThat(mockMvc.post().uri("/api/v1/pokemon/25/local").with(jwt()))
            .hasStatus(409)
            .bodyJson().extractingPath("$.message").isEqualTo("Pokémon #25 is already in the local database");
    }

    // Our records are addressed by Pokédex number only (D-040).
    @Test
    void shouldAnswerASyncByNameWith400() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("pikachu")), any(), any()))
            .willThrow(new InvalidPokedexNumberException(PokedexNumber.MIN_VALUE));

        assertThat(mockMvc.post().uri("/api/v1/pokemon/pikachu/local").with(jwt()))
            .hasStatus(400)
            .bodyJson().extractingPath("$.message").isEqualTo("Pokédex number must be a whole number, at least 1");
    }

    @Test
    void shouldAnswerASyncWith503WhenTheDataIsUnavailable() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("25")), any(), any()))
            .willThrow(new PokemonDataUnavailableException("PokeAPI is unavailable right now"));

        assertThat(mockMvc.post().uri("/api/v1/pokemon/25/local").with(jwt())).hasStatus(503);
    }

    @Test
    void shouldShowTheLocalRecordToAnyone() {
        given(getLocalPokemonUseCase.execute(new GetLocalPokemonInput("25")))
            .willReturn(new LocalPokemonOutput(25, "Pica", "Kanto", List.of("mascot", "starter"), NOW, NOW));

        assertThat(mockMvc.get().uri("/api/v1/pokemon/25/local"))
            .hasStatusOk()
            .bodyJson()
            .isStrictlyEqualTo("""
                { "pokedexNumber": 25, "localizedName": "Pica", "region": "Kanto", "tags": [ "mascot", "starter" ],
                  "syncedAt": "2026-01-15T10:00:00Z", "updatedAt": "2026-01-15T10:00:00Z" }
                """);
    }

    @Test
    void shouldAnswerTheLocalRecordByNameWith400() {
        given(getLocalPokemonUseCase.execute(new GetLocalPokemonInput("pikachu")))
            .willThrow(new InvalidPokedexNumberException(PokedexNumber.MIN_VALUE));

        assertThat(mockMvc.get().uri("/api/v1/pokemon/pikachu/local")).hasStatus(400);
    }

    @Test
    void shouldAnswerTheLocalRecordOfAPokemonThatWasNeverSyncedWith404() {
        given(getLocalPokemonUseCase.execute(new GetLocalPokemonInput("26")))
            .willThrow(new LocalPokemonNotFoundException(new PokedexNumber(26)));

        assertThat(mockMvc.get().uri("/api/v1/pokemon/26/local"))
            .hasStatus(404)
            .bodyJson().extractingPath("$.message").isEqualTo("Pokémon #26 is not in the local database");
    }

    // ---- editing our record (US-04) -----------------------------------------------------------------

    private static final String EDIT = """
        { "localizedName": "Pikachu BR", "region": "Kanto", "tags": [ "starter", "electric" ] }
        """;

    @Test
    void shouldEditOurRecordForASignedInUser() {
        given(updateLocalPokemonUseCase.execute(
            new UpdateLocalPokemonInput("25", "Pikachu BR", "Kanto", List.of("starter", "electric")), NOW))
            .willReturn(new LocalPokemonOutput(25, "Pikachu BR", "Kanto", List.of("electric", "starter"), NOW, NOW));

        assertThat(mockMvc.put().uri("/api/v1/pokemon/25/local").with(jwt()).contentType(APPLICATION_JSON).content(EDIT))
            .hasStatusOk()
            .bodyJson()
            .isStrictlyEqualTo("""
                { "pokedexNumber": 25, "localizedName": "Pikachu BR", "region": "Kanto", "tags": [ "electric", "starter" ],
                  "syncedAt": "2026-01-15T10:00:00Z", "updatedAt": "2026-01-15T10:00:00Z" }
                """);
    }

    @Test
    void shouldRequireATokenToEdit() {
        assertThat(mockMvc.put().uri("/api/v1/pokemon/25/local").contentType(APPLICATION_JSON).content(EDIT))
            .hasStatus(401)
            .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    @Test
    void shouldAnswerAMalformedEditWith400WithoutEchoingIt() {
        assertThat(mockMvc.put().uri("/api/v1/pokemon/25/local").with(jwt()).contentType(APPLICATION_JSON)
            .content("{ \"tags\": [ \"starter\" "))
            .hasStatus(400)
            .bodyJson().extractingPath("$.message").isEqualTo("Malformed JSON request body");
        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    // Sizes are checked at the edge too, so each field gets its own message (D-028).
    @Test
    void shouldAnswerAnEditOverTheSizeLimitsWith400NamingEachField() {
        var tooManyTags = "[" + "\"tag\",".repeat(10) + "\"tag\"]";
        var body = "{ \"region\": \"" + "a".repeat(101) + "\", \"tags\": " + tooManyTags + " }";

        assertThat(mockMvc.put().uri("/api/v1/pokemon/25/local").with(jwt()).contentType(APPLICATION_JSON).content(body))
            .hasStatus(400)
            .bodyJson().extractingPath("$.fieldErrors[*].field").asArray().containsExactlyInAnyOrder("region", "tags");
        verifyNoInteractions(updateLocalPokemonUseCase);
    }

    // The tag's format is the domain's rule (D-028), so its 400 comes from the use case.
    @Test
    void shouldAnswerAnEditWithAnInvalidTagWith400() {
        given(updateLocalPokemonUseCase.execute(any(), any())).willThrow(new InvalidTagException(Tag.MAX_LENGTH));

        assertThat(mockMvc.put().uri("/api/v1/pokemon/25/local").with(jwt()).contentType(APPLICATION_JSON).content(EDIT))
            .hasStatus(400)
            .bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void shouldAnswerAnEditOfAPokemonThatWasNeverSyncedWith404() {
        given(updateLocalPokemonUseCase.execute(any(), any())).willThrow(new LocalPokemonNotFoundException(new PokedexNumber(25)));

        assertThat(mockMvc.put().uri("/api/v1/pokemon/25/local").with(jwt()).contentType(APPLICATION_JSON).content(EDIT))
            .hasStatus(404);
    }

    @Test
    void shouldAnswerAnEditOfARecordChangedMeanwhileWith409() {
        given(updateLocalPokemonUseCase.execute(any(), any()))
            .willThrow(new LocalPokemonModifiedConcurrentlyException(new PokedexNumber(25)));

        assertThat(mockMvc.put().uri("/api/v1/pokemon/25/local").with(jwt()).contentType(APPLICATION_JSON).content(EDIT))
            .hasStatus(409)
            .bodyJson().extractingPath("$.message")
            .isEqualTo("Pokémon #25 was changed by someone else in the meantime. Reload it and try again");
    }

    @Test
    void shouldAnswerAnEditWith503WhenTheDataIsUnavailable() {
        given(updateLocalPokemonUseCase.execute(any(), any())).willThrow(new LocalPokemonDataUnavailableException(new RuntimeException()));

        assertThat(mockMvc.put().uri("/api/v1/pokemon/25/local").with(jwt()).contentType(APPLICATION_JSON).content(EDIT))
            .hasStatus(503);
    }

    // ---- removing our record (CRUD-D) -------------------------------------------------------------

    @Test
    void shouldRemoveOurRecordForASignedInUser() {
        assertThat(mockMvc.delete().uri("/api/v1/pokemon/25/local").with(jwt())).hasStatus(204);

        then(removeLocalPokemonUseCase).should().execute(new RemoveLocalPokemonInput("25"));
    }

    @Test
    void shouldRequireATokenToRemove() {
        assertThat(mockMvc.delete().uri("/api/v1/pokemon/25/local")).hasStatus(401);
        verifyNoInteractions(removeLocalPokemonUseCase);
    }

    @Test
    void shouldAnswerARemovalByNameWith400() {
        willThrow(new InvalidPokedexNumberException(PokedexNumber.MIN_VALUE))
            .given(removeLocalPokemonUseCase).execute(new RemoveLocalPokemonInput("pikachu"));

        assertThat(mockMvc.delete().uri("/api/v1/pokemon/pikachu/local").with(jwt())).hasStatus(400);
    }

    // Removing what was never synced must not answer 204 as if something was removed.
    @Test
    void shouldAnswerARemovalOfAPokemonThatWasNeverSyncedWith404() {
        willThrow(new LocalPokemonNotFoundException(new PokedexNumber(26)))
            .given(removeLocalPokemonUseCase).execute(new RemoveLocalPokemonInput("26"));

        assertThat(mockMvc.delete().uri("/api/v1/pokemon/26/local").with(jwt())).hasStatus(404);
    }

    @Test
    void shouldAnswerARemovalWith503WhenTheDataIsUnavailable() {
        willThrow(new LocalPokemonDataUnavailableException(new RuntimeException()))
            .given(removeLocalPokemonUseCase).execute(new RemoveLocalPokemonInput("25"));

        assertThat(mockMvc.delete().uri("/api/v1/pokemon/25/local").with(jwt())).hasStatus(503);
    }

    // One Pokémon for the client: our record rides along with the canonical data (D-030).
    @Test
    void shouldIncludeOurRecordInTheDetailWhenThePokemonIsSynced() {
        given(getPokemonUseCase.execute(new GetPokemonInput("pikachu"))).willReturn(new PokemonDetailOutput(25, "pikachu",
            "Pica", "Mouse Pokémon", new BigDecimal("0.4"), new BigDecimal("6.0"), "https://img/25.png",
            "https://img/25-art.png", List.of("electric"), List.of(), List.of(), "It keeps its tail raised.",
            new EvolutionStageOutput("pikachu", 25, List.of()),
            new LocalAttributesOutput("Pica", "Kanto", List.of("starter"), NOW, NOW)));

        assertThat(mockMvc.get().uri("/api/v1/pokemon/pikachu"))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                { "name": "pikachu",
                  "local": { "localizedName": "Pica", "region": "Kanto", "tags": [ "starter" ],
                             "syncedAt": "2026-01-15T10:00:00Z", "updatedAt": "2026-01-15T10:00:00Z" } }
                """);
    }
}
