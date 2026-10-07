package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import com.sun.net.httpserver.HttpServer;
import dev.guilhermeds.backend.domain.source.PokemonSourceUnavailableException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A real, slow HTTP server, so the timeout from application.yaml is what stops the call: a mock
 * server can only pretend a timeout happened.
 */
@SpringBootTest
@Testcontainers
class PokeApiClientTimeoutIT {

    private static final Duration SLOW_ANSWER = Duration.ofSeconds(6);
    private static final HttpServer SLOW_POKEAPI = startSlowPokeApi();

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private PokeApiClient client;

    @DynamicPropertySource
    static void pointPokeApiAtTheSlowServer(DynamicPropertyRegistry registry) {
        registry.add("pokeapi.base-url", () -> "http://localhost:" + SLOW_POKEAPI.getAddress().getPort());
    }

    @AfterAll
    static void stopSlowPokeApi() {
        SLOW_POKEAPI.stop(0);
    }

    @Test
    void shouldGiveUpOnASlowPokeApiAndReportItUnavailable() {
        var started = System.nanoTime();

        assertThatThrownBy(() -> client.fetchPokemon("25")).isInstanceOf(PokemonSourceUnavailableException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(SLOW_ANSWER);
    }

    private static HttpServer startSlowPokeApi() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                try {
                    Thread.sleep(SLOW_ANSWER);
                    var body = "{\"id\":25,\"name\":\"pikachu\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
