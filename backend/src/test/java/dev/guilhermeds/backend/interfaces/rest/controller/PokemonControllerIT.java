package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.application.dto.AbilityOutput;
import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.application.dto.EvolutionStageOutput;
import dev.guilhermeds.backend.application.dto.GetPokemonInput;
import dev.guilhermeds.backend.application.dto.LocalPokemonOutput;
import dev.guilhermeds.backend.application.dto.PageOutput;
import dev.guilhermeds.backend.application.dto.PokemonDetailOutput;
import dev.guilhermeds.backend.application.dto.PokemonSummaryOutput;
import dev.guilhermeds.backend.application.dto.StatOutput;
import dev.guilhermeds.backend.application.dto.SyncPokemonInput;
import dev.guilhermeds.backend.application.usecase.BrowsePokemonUseCase;
import dev.guilhermeds.backend.application.usecase.GetPokemonUseCase;
import dev.guilhermeds.backend.application.usecase.SyncPokemonUseCase;
import dev.guilhermeds.backend.domain.exception.InvalidPageRequestException;
import dev.guilhermeds.backend.domain.exception.InvalidPokemonIdentifierException;
import dev.guilhermeds.backend.domain.exception.PokemonAlreadySyncedException;
import dev.guilhermeds.backend.domain.exception.PokemonDataUnavailableException;
import dev.guilhermeds.backend.domain.exception.PokemonNotFoundException;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
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
import static org.mockito.Mockito.verifyNoInteractions;
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
                    "evolvesTo": [ { "speciesName": "pikachu", "pokedexNumber": 25, "evolvesTo": [] } ] }
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

    // Location uses the Pokédex number, whatever the client called the Pokémon.
    @Test
    void shouldSyncAPokemonForASignedInUser() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("pikachu")), any(), eq(NOW)))
            .willReturn(new LocalPokemonOutput(25, null, null, List.of(), NOW, NOW));

        assertThat(mockMvc.post().uri("/api/v1/pokemon/pikachu/local").with(jwt()))
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
        assertThat(mockMvc.post().uri("/api/v1/pokemon/pikachu/local"))
            .hasStatus(401)
            .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
        verifyNoInteractions(syncPokemonUseCase);
    }

    @Test
    void shouldAnswerASyncOfAnUnknownPokemonWith404() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("missingno")), any(), any()))
            .willThrow(new PokemonNotFoundException(new PokemonIdentifier("missingno")));

        assertThat(mockMvc.post().uri("/api/v1/pokemon/missingno/local").with(jwt())).hasStatus(404);
    }

    @Test
    void shouldAnswerASecondSyncWith409() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("pikachu")), any(), any()))
            .willThrow(new PokemonAlreadySyncedException(new PokedexNumber(25)));

        assertThat(mockMvc.post().uri("/api/v1/pokemon/pikachu/local").with(jwt()))
            .hasStatus(409)
            .bodyJson().extractingPath("$.message").isEqualTo("Pokémon #25 is already in the local database");
    }

    @Test
    void shouldAnswerASyncOfAMalformedIdentifierWith400() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("pika!")), any(), any()))
            .willThrow(new InvalidPokemonIdentifierException());

        assertThat(mockMvc.post().uri("/api/v1/pokemon/pika!/local").with(jwt())).hasStatus(400);
    }

    @Test
    void shouldAnswerASyncWith503WhenTheDataIsUnavailable() {
        given(syncPokemonUseCase.execute(eq(new SyncPokemonInput("pikachu")), any(), any()))
            .willThrow(new PokemonDataUnavailableException("PokeAPI is unavailable right now"));

        assertThat(mockMvc.post().uri("/api/v1/pokemon/pikachu/local").with(jwt())).hasStatus(503);
    }
}
