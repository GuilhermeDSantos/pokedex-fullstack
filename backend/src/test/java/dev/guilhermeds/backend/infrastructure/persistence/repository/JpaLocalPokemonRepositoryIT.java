package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.domain.exception.ConflictException;
import dev.guilhermeds.backend.domain.exception.LocalPokemonModifiedConcurrentlyException;
import dev.guilhermeds.backend.domain.exception.PokemonAlreadySyncedException;
import dev.guilhermeds.backend.domain.model.CustomAttributes;
import dev.guilhermeds.backend.domain.model.LocalPokemon;
import dev.guilhermeds.backend.domain.model.LocalPokemonId;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.Tag;
import dev.guilhermeds.backend.fixture.LocalPokemonFixture;
import dev.guilhermeds.backend.infrastructure.persistence.mapper.LocalPokemonEntityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.NOW;
import static dev.guilhermeds.backend.fixture.LocalPokemonFixture.PIKACHU_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private LocalPokemonJpaRepository jpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    // Two syncs of #25 at the same time both pass the use case's check; the unique number decides.
    @Test
    void shouldTranslateASecondRecordOfTheSamePokemonIntoAConflict() {
        repository.save(LocalPokemonFixture.syncedPikachu());
        var secondPikachu = LocalPokemon.create(
            new LocalPokemonId(UUID.fromString("00000000-0000-0000-0000-000000000099")), PIKACHU_NUMBER, NOW);

        assertThatThrownBy(() -> repository.save(secondPikachu))
            .isInstanceOf(PokemonAlreadySyncedException.class)
            .isInstanceOf(ConflictException.class)
            .hasMessage("Pokémon #25 is already in the local database");
    }

    // Each edit loads the record and saves it back (US-04): the second one must not trip over the first.
    @Test
    void shouldSaveEveryEditOfTheSameRecord() {
        repository.save(LocalPokemonFixture.syncedPikachu());
        var lastEdit = new CustomAttributes("Pica", "Johto", Set.of(new Tag("electric")));

        edit(new CustomAttributes("Pikachu BR", "Kanto", Set.of(new Tag("starter"))), NOW.plusSeconds(60));
        edit(lastEdit, NOW.plusSeconds(120));

        assertThat(repository.findByPokedexNumber(PIKACHU_NUMBER)).hasValueSatisfying(reloaded -> {
            assertThat(reloaded.getCustomAttributes()).isEqualTo(lastEdit);
            assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW.plusSeconds(120));
        });
    }

    private void edit(CustomAttributes attributes, Instant now) {
        var pikachu = repository.getByPokedexNumber(PIKACHU_NUMBER);
        pikachu.updateCustomAttributes(attributes, now);
        repository.save(pikachu);
    }

    // Removed with its tags; the Pokémon can then be synced again (CRUD-D).
    @Test
    void shouldDeleteTheRecordWithItsTags() {
        var pikachu = repository.save(LocalPokemonFixture.renamedPikachu());

        repository.delete(pikachu);

        assertThat(repository.findByPokedexNumber(PIKACHU_NUMBER)).isEmpty();
        assertThat(repository.save(LocalPokemonFixture.syncedPikachu()).getCustomAttributes().tags()).isEmpty();
    }

    // Two people edit Eevee at once: the one who saves last is told, instead of silently overwriting (D-011).
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRefuseAnEditOfARecordChangedSinceItWasRead() {
        var eevee = new PokedexNumber(133);
        var transaction = new TransactionTemplate(transactionManager);
        var meanwhile = new TransactionTemplate(transactionManager);
        meanwhile.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var record = LocalPokemon.create(new LocalPokemonId(UUID.fromString("00000000-0000-0000-0000-000000000133")), eevee, NOW);
        transaction.executeWithoutResult(status -> repository.save(record));
        try {
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                var mine = repository.getByPokedexNumber(eevee);
                meanwhile.executeWithoutResult(inner -> {
                    var theirs = repository.getByPokedexNumber(eevee);
                    theirs.updateCustomAttributes(new CustomAttributes("Evoli", null, Set.of()), NOW.plusSeconds(60));
                    repository.save(theirs);
                });
                mine.updateCustomAttributes(new CustomAttributes("Eievui", null, Set.of()), NOW.plusSeconds(120));
                repository.save(mine);
            }))
                .isInstanceOf(LocalPokemonModifiedConcurrentlyException.class)
                .isInstanceOf(ConflictException.class)
                .hasMessage("Pokémon #133 was changed by someone else in the meantime. Reload it and try again");
        } finally {
            transaction.executeWithoutResult(status -> jpaRepository.deleteById(record.getId().value()));
        }
    }
}
