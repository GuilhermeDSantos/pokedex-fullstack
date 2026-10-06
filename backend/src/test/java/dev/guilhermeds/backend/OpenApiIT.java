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
 * The API documentation is public, so the routes can be tried from the browser (D-016).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OpenApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void shouldPublishTheOpenApiContractWithoutAToken() {
        assertThat(mockMvc.get().uri("/v3/api-docs"))
            .hasStatusOk()
            .bodyJson().extractingPath("$.paths['/api/v1/auth/login'].post").isNotNull();
    }

    @Test
    void shouldServeTheSwaggerUiWithoutAToken() {
        assertThat(mockMvc.get().uri("/swagger-ui.html"))
            .hasStatus3xxRedirection()
            .hasRedirectedUrl("/swagger-ui/index.html");
        assertThat(mockMvc.get().uri("/swagger-ui/index.html"))
            .hasStatusOk()
            .bodyText().contains("swagger-ui");
    }

    @Test
    void shouldDescribeTheApiAndMarkProtectedRoutesWithTheBearerScheme() {
        var contract = mockMvc.get().uri("/v3/api-docs").exchange();

        assertThat(contract).bodyJson().extractingPath("$.info.title").isEqualTo("Pokémon Catalog API");

        assertThat(contract).bodyJson().extractingPath("$.components.securitySchemes.bearer-jwt.scheme")
            .isEqualTo("bearer");
        assertThat(contract).bodyJson().extractingPath("$.paths['/api/v1/auth/me'].get.security[0].bearer-jwt")
            .isNotNull();
        assertThat(contract).bodyJson().doesNotHavePath("$.paths['/api/v1/auth/login'].post.security");
    }
}
