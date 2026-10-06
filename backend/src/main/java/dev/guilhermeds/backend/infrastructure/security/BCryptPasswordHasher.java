package dev.guilhermeds.backend.infrastructure.security;

import dev.guilhermeds.backend.application.port.PasswordHasher;
import dev.guilhermeds.backend.domain.model.PasswordHash;
import dev.guilhermeds.backend.domain.model.RawPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(RawPassword password) {
        return new PasswordHash(encoder.encode(password.value()));
    }

    @Override
    public boolean matches(RawPassword candidate, PasswordHash hash) {
        return encoder.matches(candidate.value(), hash.value());
    }
}
