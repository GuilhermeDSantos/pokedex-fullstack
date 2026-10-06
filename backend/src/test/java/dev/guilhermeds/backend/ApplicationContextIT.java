package dev.guilhermeds.backend;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every input port must resolve to exactly one bean. The ports are discovered, not listed, so a
 * use case added without its {@code @Bean} in {@code UseCaseConfig} fails here.
 */
@SpringBootTest
@Testcontainers
class ApplicationContextIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldProvideOneBeanForEveryInputPort() {
        var inputPorts = inputPorts();

        assertThat(inputPorts).isNotEmpty();
        assertThat(inputPorts)
            .allSatisfy(port -> assertThat(context.getBeanNamesForType(port)).as(port.getSimpleName()).hasSize(1));
    }

    private static List<Class<?>> inputPorts() {
        return new ClassFileImporter().importPackages("dev.guilhermeds.backend.application.usecase").stream()
            .filter(JavaClass::isInterface)
            .filter(type -> type.getSimpleName().endsWith("UseCase"))
            .<Class<?>>map(JavaClass::reflect)
            .toList();
    }
}
