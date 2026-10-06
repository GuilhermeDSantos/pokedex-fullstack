package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.AuthenticateUserInput;
import dev.guilhermeds.backend.application.mapper.UserAccountMapper;
import dev.guilhermeds.backend.application.port.AccessToken;
import dev.guilhermeds.backend.application.port.PasswordHasher;
import dev.guilhermeds.backend.application.port.TokenIssuer;
import dev.guilhermeds.backend.domain.exception.InvalidCredentialsException;
import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.RawPassword;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static dev.guilhermeds.backend.fixture.UserAccountFixture.NOW;
import static dev.guilhermeds.backend.fixture.UserAccountFixture.ash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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

    @Test
    void shouldIssueATokenWhenThePasswordMatches() {
        var ash = ash();
        var token = new AccessToken("signed.jwt.value", NOW.plusSeconds(3600));
        given(repository.findByEmail(new Email("ash@pallet.town"))).willReturn(Optional.of(ash));
        given(passwordHasher.matches(new RawPassword("pikachu123"), ash.getPasswordHash())).willReturn(true);
        given(tokenIssuer.issue(ash, NOW)).willReturn(token);

        var output = interactor.execute(new AuthenticateUserInput(" ASH@pallet.town", "pikachu123"), NOW);

        assertThat(output.accessToken()).isEqualTo("signed.jwt.value");
        assertThat(output.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
    }
}
