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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL going down is treated like PokeAPI going down: a 503, not a 500, and quickly. The
 * container is stopped for real, so the context is thrown away afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext
class DatabaseUnavailableIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void shouldAnswerASignInWith503WhenTheDatabaseIsDown() {
        postgres.stop();
        var started = System.nanoTime();

        assertThat(mockMvc.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "ash@pallet.town", "password": "pikachu123" }
                    """))
            .hasStatus(503)
            .bodyJson().extractingPath("$.code").isEqualTo("DATA_UNAVAILABLE");
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(10));
    }
}
