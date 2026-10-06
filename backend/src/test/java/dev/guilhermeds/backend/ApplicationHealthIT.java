package dev.guilhermeds.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the whole stack: the context starts against a real PostgreSQL, Flyway and
 * Hibernate validation run, and the health endpoint — the one Docker probes — is public.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApplicationHealthIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void shouldReportUpWithoutAuthenticationWhenDatabaseIsReachable() {
        assertThat(mockMvc.get().uri("/actuator/health"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldNotExposeOtherActuatorEndpoints() {
        assertThat(mockMvc.get().uri("/actuator/env"))
            .hasStatus4xxClientError();
    }
}
