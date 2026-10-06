package dev.guilhermeds.backend.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A registered user. New accounts come from {@link #register}; {@link #builder()} only rebuilds an
 * account that was already valid when it was stored (persistence mapper).
 */
public class UserAccount {

    private final UserId id;
    private final Email email;
    private final DisplayName displayName;
    private final PasswordHash passwordHash;
    private final Instant createdAt;

    private UserAccount(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id must not be null");
        this.email = Objects.requireNonNull(builder.email, "email must not be null");
        this.displayName = Objects.requireNonNull(builder.displayName, "displayName must not be null");
        this.passwordHash = Objects.requireNonNull(builder.passwordHash, "passwordHash must not be null");
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt must not be null");
    }

    public static UserAccount register(UserId id, Email email, DisplayName displayName,
                                      PasswordHash passwordHash, Instant now) {
        return builder()
            .id(id)
            .email(email)
            .displayName(displayName)
            .passwordHash(passwordHash)
            .createdAt(now)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public UserId getId() { return id; }
    public Email getEmail() { return email; }
    public DisplayName getDisplayName() { return displayName; }
    public PasswordHash getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof UserAccount other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "UserAccount{id=" + id.value() + "}";
    }

    public static final class Builder {

        private UserId id;
        private Email email;
        private DisplayName displayName;
        private PasswordHash passwordHash;
        private Instant createdAt;

        private Builder() {
        }

        public Builder id(UserId id) { this.id = id; return this; }
        public Builder email(Email email) { this.email = email; return this; }
        public Builder displayName(DisplayName displayName) { this.displayName = displayName; return this; }
        public Builder passwordHash(PasswordHash passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public UserAccount build() {
            return new UserAccount(this);
        }
    }
}
