package dev.guilhermeds.backend.infrastructure.config;

import dev.guilhermeds.backend.interfaces.rest.security.ErrorResponseAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The route policy (D-030) against the real filter chain. The probe controller only answers 200, so
 * any other status comes from security.
 */
@WebMvcTest(SecurityConfigIT.ProbeController.class)
@Import({SecurityConfig.class, JwtConfig.class, ErrorResponseAuthenticationEntryPoint.class,
    SecurityConfigIT.ProbeController.class})
class SecurityConfigIT {

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void shouldAnswerAMissingTokenWithAnErrorResponse() {
        assertThat(mockMvc.post().uri("/api/v1/pokemon/25/local"))
            .hasStatus(401)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "UNAUTHENTICATED", "message": "Authentication is required to access this resource",
                  "fieldErrors": [] }
                """);
    }

    @Test
    void shouldAnswerAnInvalidTokenWithAnErrorResponseThatDoesNotSayWhy() {
        assertThat(mockMvc.post().uri("/api/v1/pokemon/25/local").header("Authorization", "Bearer not-a-jwt"))
            .hasStatus(401)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "UNAUTHENTICATED", "message": "Authentication is required to access this resource",
                  "fieldErrors": [] }
                """);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/pokemon", "/api/v1/pokemon/25", "/api/v1/pokemon/pikachu/local"})
    void shouldLetAnyoneReadThePokemon(String uri) {
        assertThat(mockMvc.get().uri(uri)).hasStatusOk();
    }

    @RestController
    static class ProbeController {

        @GetMapping({"/api/v1/pokemon", "/api/v1/pokemon/{identifier}", "/api/v1/pokemon/{identifier}/local"})
        void read() {
        }

        @PostMapping({"/api/v1/pokemon/{identifier}/local", "/api/v1/pokemon/{identifier}",
            "/api/v1/auth/register", "/api/v1/auth/login"})
        void create() {
        }

        @PutMapping("/api/v1/pokemon/{identifier}/local")
        void update() {
        }

        @DeleteMapping("/api/v1/pokemon/{identifier}/local")
        void remove() {
        }

        @GetMapping("/api/v1/auth/me")
        void me() {
        }
    }
}
