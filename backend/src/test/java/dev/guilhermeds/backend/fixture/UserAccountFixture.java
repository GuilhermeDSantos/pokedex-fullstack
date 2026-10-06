package dev.guilhermeds.backend.fixture;

import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.FullName;
import dev.guilhermeds.backend.domain.model.PasswordHash;
import dev.guilhermeds.backend.domain.model.UserAccount;
import dev.guilhermeds.backend.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

public final class UserAccountFixture {

    public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    public static final UserId ASH_ID = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    public static final UserId MISTY_ID = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    private UserAccountFixture() {
    }

    public static UserAccount ash() {
        return UserAccount.register(ASH_ID, new Email("ash@pallet.town"), new FullName("Ash Ketchum"),
            new PasswordHash("$2a$10$abcdefghijklmnopqrstuuVwXyZ0123456789abcdefghijklmnopq"), NOW);
    }

    public static UserAccount withEmail(UserId id, String email) {
        return UserAccount.register(id, new Email(email), new FullName("Misty"),
            new PasswordHash("$2a$10$abcdefghijklmnopqrstuuVwXyZ0123456789abcdefghijklmnopq"), NOW);
    }
}
