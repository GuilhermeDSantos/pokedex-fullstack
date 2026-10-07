package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.application.dto.AbilityOutput;
import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.application.dto.PageOutput;
import dev.guilhermeds.backend.application.dto.PokemonSummaryOutput;
import dev.guilhermeds.backend.application.usecase.BrowsePokemonUseCase;
import dev.guilhermeds.backend.domain.exception.InvalidPageRequestException;
import dev.guilhermeds.backend.infrastructure.config.JwtConfig;
import dev.guilhermeds.backend.infrastructure.config.SecurityConfig;
import dev.guilhermeds.backend.interfaces.rest.mapper.PokemonRestMapper;
import dev.guilhermeds.backend.interfaces.rest.security.ErrorResponseAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(PokemonController.class)
@Import({PokemonRestMapper.class, SecurityConfig.class, JwtConfig.class, ErrorResponseAuthenticationEntryPoint.class})
class PokemonControllerIT {

    private static final PokemonSummaryOutput PIKACHU = new PokemonSummaryOutput(25, "pikachu", "https://img/25.png",
        "Mouse Pokémon", new BigDecimal("6.0"), List.of("electric"),
        List.of(new AbilityOutput("static", false), new AbilityOutput("lightning-rod", true)));

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private BrowsePokemonUseCase browsePokemonUseCase;

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
}
