package dev.guilhermeds.backend.application.port;

import dev.guilhermeds.backend.domain.model.PasswordHash;
import dev.guilhermeds.backend.domain.model.RawPassword;

public interface PasswordHasher {

    PasswordHash hash(RawPassword password);

    boolean matches(RawPassword candidate, PasswordHash hash);
}
