package dev.guilhermeds.backend.infrastructure.config;

import dev.guilhermeds.backend.fixture.UserAccountFixture;
import dev.guilhermeds.backend.infrastructure.security.JwtTokenIssuer;
import dev.guilhermeds.backend.interfaces.rest.security.ErrorResponseAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * The route policy (D-030) against the real filter chain. The probe controller only answers 200, so
 * any other status comes from security.
 */
@WebMvcTest(SecurityConfigIT.ProbeController.class)
@Import({SecurityConfig.class, JwtConfig.class, JwtTokenIssuer.class, ErrorResponseAuthenticationEntryPoint.class,
    SecurityConfigIT.ProbeController.class})
class SecurityConfigIT {

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private JwtTokenIssuer tokenIssuer;

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

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/register", "/api/v1/auth/login"})
    void shouldLetAnyoneRegisterAndSignIn(String uri) {
        assertThat(mockMvc.post().uri(uri)).hasStatusOk();
    }

    // An expired token left in the browser must not break pages anyone can see (D-036).
    @ParameterizedTest
    @CsvSource({
        "GET,  /api/v1/pokemon",
        "GET,  /api/v1/pokemon/25",
        "GET,  /api/v1/pokemon/25/local",
        "POST, /api/v1/auth/register",
        "POST, /api/v1/auth/login"
    })
    void shouldIgnoreAnInvalidTokenOnPublicRoutes(String method, String uri) {
        assertThat(mockMvc.method(HttpMethod.valueOf(method)).uri(uri).header("Authorization", "Bearer expired-or-garbage"))
            .hasStatusOk();
    }

    // The last row is a route nobody declared: the policy is closed by default.
    @ParameterizedTest
    @CsvSource({
        "POST,   /api/v1/pokemon/25/local",
        "PUT,    /api/v1/pokemon/25/local",
        "DELETE, /api/v1/pokemon/25/local",
        "GET,    /api/v1/auth/me",
        "POST,   /api/v1/pokemon/25"
    })
    void shouldRequireATokenForWritesAndTheCurrentUser(String method, String uri) {
        assertThat(mockMvc.method(HttpMethod.valueOf(method)).uri(uri)).hasStatus(401);
    }

    @ParameterizedTest
    @CsvSource({
        "POST,   /api/v1/pokemon/25/local",
        "PUT,    /api/v1/pokemon/25/local",
        "DELETE, /api/v1/pokemon/25/local",
        "GET,    /api/v1/auth/me"
    })
    void shouldLetAnAuthenticatedUserWriteAndSeeTheCurrentUser(String method, String uri) {
        assertThat(mockMvc.method(HttpMethod.valueOf(method)).uri(uri).with(jwt())).hasStatusOk();
    }

    // jwt() bypasses the bearer token filter, so this is the test that reads a real Authorization header.
    @Test
    void shouldAcceptARealTokenOnAProtectedRoute() {
        var token = tokenIssuer.issue(UserAccountFixture.ash(), Instant.now()).value();

        assertThat(mockMvc.post().uri("/api/v1/pokemon/25/local").header("Authorization", "Bearer " + token))
            .hasStatusOk();
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
