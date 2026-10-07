package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.fixture.LocalPokemonFixture;
import dev.guilhermeds.backend.infrastructure.persistence.mapper.LocalPokemonEntityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.PIKACHU_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaLocalPokemonRepository.class, LocalPokemonEntityMapper.class})
@Testcontainers
class JpaLocalPokemonRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JpaLocalPokemonRepository repository;

    @Test
    void shouldSaveAndReloadTheWholeRecordWithItsTags() {
        var pikachu = LocalPokemonFixture.renamedPikachu();

        repository.save(pikachu);

        assertThat(repository.findByPokedexNumber(PIKACHU_NUMBER)).hasValueSatisfying(reloaded -> {
            assertThat(reloaded.getId()).isEqualTo(pikachu.getId());
            assertThat(reloaded.getCustomAttributes()).isEqualTo(pikachu.getCustomAttributes());
            assertThat(reloaded.getSyncedAt()).isEqualTo(pikachu.getSyncedAt());
            assertThat(reloaded.getUpdatedAt()).isEqualTo(pikachu.getUpdatedAt());
        });
    }
}
