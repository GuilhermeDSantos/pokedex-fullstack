package dev.guilhermeds.backend.infrastructure.transaction;

import dev.guilhermeds.backend.application.port.UnitOfWork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class SpringUnitOfWorkIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private UnitOfWork unitOfWork;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void createProbeTable() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS uow_probe (name VARCHAR(20))");
        jdbc.execute("TRUNCATE uow_probe");
    }

    @Test
    void shouldCommitAndReturnTheResultWhenWorkSucceeds() {
        var result = unitOfWork.inTransaction(() -> {
            jdbc.update("INSERT INTO uow_probe VALUES ('pikachu')");
            return "done";
        });

        assertThat(result).isEqualTo("done");
        assertThat(probeRows()).isEqualTo(1);
    }

    private int probeRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM uow_probe", Integer.class);
    }
}
