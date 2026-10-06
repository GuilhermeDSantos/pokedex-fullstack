package dev.guilhermeds.backend.infrastructure.config;

import dev.guilhermeds.backend.application.mapper.UserAccountMapper;
import dev.guilhermeds.backend.application.port.PasswordHasher;
import dev.guilhermeds.backend.application.port.TokenIssuer;
import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.application.usecase.AuthenticateUserInteractor;
import dev.guilhermeds.backend.application.usecase.AuthenticateUserUseCase;
import dev.guilhermeds.backend.application.usecase.GetCurrentUserInteractor;
import dev.guilhermeds.backend.application.usecase.GetCurrentUserUseCase;
import dev.guilhermeds.backend.application.usecase.RegisterUserInteractor;
import dev.guilhermeds.backend.application.usecase.RegisterUserUseCase;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The composition root: the only place that knows the interactors. Each bean is typed as its input port.
@Configuration
public class UseCaseConfig {

    @Bean
    UserAccountMapper userAccountMapper() {
        return new UserAccountMapper();
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserAccountRepository repository, PasswordHasher passwordHasher,
                                            UserAccountMapper mapper, UnitOfWork unitOfWork) {
        return new RegisterUserInteractor(repository, passwordHasher, mapper, unitOfWork);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(UserAccountRepository repository, PasswordHasher passwordHasher,
                                                    TokenIssuer tokenIssuer, UserAccountMapper mapper) {
        return new AuthenticateUserInteractor(repository, passwordHasher, tokenIssuer, mapper);
    }

    @Bean
    GetCurrentUserUseCase getCurrentUserUseCase(UserAccountRepository repository, UserAccountMapper mapper) {
        return new GetCurrentUserInteractor(repository, mapper);
    }
}
