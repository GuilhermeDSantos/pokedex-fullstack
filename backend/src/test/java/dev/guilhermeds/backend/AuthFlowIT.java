package dev.guilhermeds.backend;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole sign-up and sign-in path with nothing simulated: real filter chain, BCrypt, the issued
 * JWT and PostgreSQL. The controller ITs only fake a token, so this is where the pieces meet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void shouldRegisterSignInAndReadTheCurrentUserWithTheIssuedToken() throws Exception {
        assertThat(mockMvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "Brock@Pewter.city", "name": "Brock", "password": "onix12345" }
                    """))
            .hasStatus(201);

        var login = mockMvc.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                { "email": "brock@pewter.city", "password": "onix12345" }
                """)
            .exchange();
        assertThat(login).hasStatusOk();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        assertThat(mockMvc.get().uri("/api/v1/auth/me").header("Authorization", "Bearer " + token))
            .hasStatusOk()
            .bodyJson()
            .isLenientlyEqualTo("""
                { "email": "brock@pewter.city", "name": "Brock" }
                """);
    }

    @Test
    void shouldAnswerAGarbageTokenWithAnErrorResponse() {
        assertThat(mockMvc.get().uri("/api/v1/auth/me").header("Authorization", "Bearer garbage"))
            .hasStatus(401)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "UNAUTHENTICATED", "message": "Authentication is required to access this resource" }
                """);
    }
}
