package com.example.tasks;

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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

// The whole application: real HTTP Basic against the seeded users, real PostgreSQL.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaskOwnershipIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvcTester mockMvc;

    // Another user's task must look exactly like one that doesn't exist, in every operation.
    @Test
    void shouldHideAlicesTaskFromBobInEveryOperation() {
        var created = mockMvc.post().uri("/api/v1/tasks").with(httpBasic("alice", "alice-pass-1"))
            .contentType(APPLICATION_JSON).content("{ \"title\": \"Alice's secret\" }").exchange();
        assertThat(created).hasStatus(201);
        var location = created.getResponse().getHeader("Location");

        assertThat(mockMvc.get().uri(location).with(httpBasic("bob", "bob-pass-1"))).hasStatus(404);
        assertThat(mockMvc.put().uri(location).with(httpBasic("bob", "bob-pass-1")).contentType(APPLICATION_JSON)
            .content("{ \"title\": \"Mine now\", \"status\": \"DONE\", \"version\": 0 }")).hasStatus(404);
        assertThat(mockMvc.delete().uri(location).with(httpBasic("bob", "bob-pass-1"))).hasStatus(404);
        assertThat(mockMvc.get().uri("/api/v1/tasks").with(httpBasic("bob", "bob-pass-1")))
            .hasStatusOk().bodyJson().extractingPath("$.totalElements").isEqualTo(0);
        assertThat(mockMvc.get().uri(location).with(httpBasic("alice", "alice-pass-1")))
            .hasStatusOk().bodyJson().extractingPath("$.title").isEqualTo("Alice's secret");
    }

    @Test
    void shouldRefuseAWrongPassword() {
        assertThat(mockMvc.get().uri("/api/v1/tasks").with(httpBasic("alice", "not-her-password")))
            .hasStatus(401)
            .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
    }
}
