package dev.guilhermeds.backend.application.port;

import dev.guilhermeds.backend.domain.model.UserAccount;

import java.time.Instant;

public interface TokenIssuer {

    AccessToken issue(UserAccount account, Instant now);
}
