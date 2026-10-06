package dev.guilhermeds.backend.infrastructure.persistence.repository;

import dev.guilhermeds.backend.domain.exception.ConflictException;
import dev.guilhermeds.backend.domain.exception.EmailAlreadyRegisteredException;
import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.UserId;
import dev.guilhermeds.backend.fixture.UserAccountFixture;
import dev.guilhermeds.backend.infrastructure.persistence.mapper.UserAccountEntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaUserAccountRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private UserAccountJpaRepository jpaRepository;

    private JpaUserAccountRepository repository;

    @BeforeEach
    void setUp() {
        // @DataJpaTest doesn't scan plain @Components, so the mapper is constructed.
        repository = new JpaUserAccountRepository(jpaRepository, new UserAccountEntityMapper());
    }

    @Test
    void shouldSaveAndReloadTheWholeAccount() {
        var account = UserAccountFixture.ash();

        repository.save(account);

        assertThat(repository.findById(account.getId())).hasValueSatisfying(reloaded -> {
            assertThat(reloaded.getEmail()).isEqualTo(account.getEmail());
            assertThat(reloaded.getName()).isEqualTo(account.getName());
            assertThat(reloaded.getPasswordHash()).isEqualTo(account.getPasswordHash());
            assertThat(reloaded.getCreatedAt()).isEqualTo(account.getCreatedAt());
        });
    }

    @Test
    void shouldFindAnAccountByEmail() {
        repository.save(UserAccountFixture.ash());

        assertThat(repository.findByEmail(new Email("ash@pallet.town")))
            .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(UserAccountFixture.ASH_ID));
        assertThat(repository.findByEmail(new Email("misty@cerulean.city"))).isEmpty();
    }

    @Test
    void shouldTranslateADuplicateEmailIntoAConflict() {
        repository.save(UserAccountFixture.ash());
        var sameEmail = UserAccountFixture.withEmail(
            new UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")), "ash@pallet.town");

        assertThatThrownBy(() -> repository.save(sameEmail))
            .isInstanceOf(EmailAlreadyRegisteredException.class)
            .isInstanceOf(ConflictException.class)
            .hasMessage("This email is already registered");
    }
}
