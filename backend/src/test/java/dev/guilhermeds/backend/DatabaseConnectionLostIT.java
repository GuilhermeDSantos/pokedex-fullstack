package dev.guilhermeds.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The database dies right after a request used it. Hikari hands out a connection used in the last
 * 500 ms without checking it, so the next query fails mid-flight instead of failing to connect.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext
class DatabaseConnectionLostIT {

    private static final String SIGN_IN = """
        { "email": "ash@pallet.town", "password": "pikachu123" }
        """;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void shouldAnswer503WhenTheDatabaseDropsAConnectionThatWasJustUsed() {
        assertThat(mockMvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(SIGN_IN))
            .hasStatus(401);
        postgres.stop();

        assertThat(mockMvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(SIGN_IN))
            .hasStatus(503)
            .bodyJson().extractingPath("$.code").isEqualTo("DATA_UNAVAILABLE");
    }
}
