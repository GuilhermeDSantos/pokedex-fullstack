package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final UserId ID = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Test
    void shouldRegisterWithTheGivenIdAndTimestamp() {
        var account = UserAccount.register(ID, new Email("ash@pallet.town"), new FullName("Ash"),
            new PasswordHash("$2a$10$hash"), NOW);

        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getEmail()).isEqualTo(new Email("ash@pallet.town"));
        assertThat(account.getName()).isEqualTo(new FullName("Ash"));
        assertThat(account.getPasswordHash()).isEqualTo(new PasswordHash("$2a$10$hash"));
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
    }
}
