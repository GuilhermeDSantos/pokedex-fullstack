package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.AuthenticateUserInput;
import dev.guilhermeds.backend.application.mapper.UserAccountMapper;
import dev.guilhermeds.backend.application.port.PasswordHasher;
import dev.guilhermeds.backend.application.port.TokenIssuer;
import dev.guilhermeds.backend.domain.exception.InvalidCredentialsException;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static dev.guilhermeds.backend.fixture.UserAccountFixture.NOW;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserInteractorTest {

    @Mock private UserAccountRepository repository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenIssuer tokenIssuer;

    private AuthenticateUserInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new AuthenticateUserInteractor(repository, passwordHasher, tokenIssuer, new UserAccountMapper());
    }

    @Test
    void shouldRefuseAnUnknownEmail() {
        assertThatThrownBy(() -> interactor.execute(new AuthenticateUserInput("gary@pallet.town", "eevee1234"), NOW))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("Invalid email or password");
    }
}
