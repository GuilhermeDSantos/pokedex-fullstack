package dev.guilhermeds.backend;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.guilhermeds.backend.application.dto.AuthenticateUserInput;
import dev.guilhermeds.backend.application.usecase.AuthenticateUserUseCase;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.repository.LocalPokemonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The application as it ships: every input port resolves to exactly one bean (the ports are
 * discovered, not listed, so a use case added without its {@code @Bean} in {@code UseCaseConfig}
 * fails here), and the migrations leave the demo data the README promises (DL-2).
 */
@SpringBootTest
@Testcontainers
class ApplicationContextIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AuthenticateUserUseCase authenticateUserUseCase;

    @Autowired
    private LocalPokemonRepository localPokemonRepository;

    @Test
    void shouldProvideOneBeanForEveryInputPort() {
        var inputPorts = inputPorts();

        assertThat(inputPorts).isNotEmpty();
        assertThat(inputPorts)
            .allSatisfy(port -> assertThat(context.getBeanNamesForType(port)).as(port.getSimpleName()).hasSize(1));
    }

    // The credentials written in the README sign in against the seed.
    @Test
    void shouldSignInTheDemoAccount() {
        var token = authenticateUserUseCase.execute(
            new AuthenticateUserInput("demo@pokemon.com", "Pikachu2026!"), Instant.parse("2026-01-15T10:00:00Z"));

        assertThat(token.accessToken()).isNotBlank();
    }

    // Pikachu stays out: the demo syncs it live.
    @Test
    void shouldStartWithTenSyncedPokemonButNotPikachu() {
        var seeded = IntStream.of(1, 4, 6, 7, 39, 52, 54, 94, 143, 149).mapToObj(PokedexNumber::new).toList();

        assertThat(localPokemonRepository.findAllByPokedexNumbers(seeded)).hasSize(10);
        assertThat(localPokemonRepository.findByPokedexNumber(new PokedexNumber(25))).isEmpty();
    }

    private static List<Class<?>> inputPorts() {
        return new ClassFileImporter().importPackages("dev.guilhermeds.backend.application.usecase").stream()
            .filter(JavaClass::isInterface)
            .filter(type -> type.getSimpleName().endsWith("UseCase"))
            .<Class<?>>map(JavaClass::reflect)
            .toList();
    }
}
