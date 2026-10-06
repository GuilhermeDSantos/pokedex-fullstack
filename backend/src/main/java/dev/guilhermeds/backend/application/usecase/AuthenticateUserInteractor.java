package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.AccessTokenOutput;
import dev.guilhermeds.backend.application.dto.AuthenticateUserInput;
import dev.guilhermeds.backend.application.mapper.UserAccountMapper;
import dev.guilhermeds.backend.application.port.PasswordHasher;
import dev.guilhermeds.backend.application.port.TokenIssuer;
import dev.guilhermeds.backend.domain.exception.InvalidCredentialsException;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;

import java.time.Instant;

public class AuthenticateUserInteractor implements AuthenticateUserUseCase {

    private final UserAccountRepository repository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final UserAccountMapper mapper;

    public AuthenticateUserInteractor(UserAccountRepository repository, PasswordHasher passwordHasher,
                                      TokenIssuer tokenIssuer, UserAccountMapper mapper) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.mapper = mapper;
    }

    @Override
    public AccessTokenOutput execute(AuthenticateUserInput input, Instant now) {
        var credentials = mapper.toCredentials(input);

        var account = repository.findByEmail(credentials.email())
            .filter(found -> passwordHasher.matches(credentials.password(), found.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

        return AccessTokenOutput.from(tokenIssuer.issue(account, now));
    }
}
