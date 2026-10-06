package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.RegisterUserInput;
import dev.guilhermeds.backend.application.mapper.UserAccountMapper;
import dev.guilhermeds.backend.application.port.PasswordHasher;
import dev.guilhermeds.backend.application.port.UnitOfWork;
import dev.guilhermeds.backend.domain.exception.EmailAlreadyRegisteredException;
import dev.guilhermeds.backend.domain.exception.WeakPasswordException;
import dev.guilhermeds.backend.domain.model.Email;
import dev.guilhermeds.backend.domain.model.PasswordHash;
import dev.guilhermeds.backend.domain.model.RawPassword;
import dev.guilhermeds.backend.domain.model.UserAccount;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Supplier;

import static dev.guilhermeds.backend.fixture.UserAccountFixture.ASH_ID;
import static dev.guilhermeds.backend.fixture.UserAccountFixture.NOW;
import static dev.guilhermeds.backend.fixture.UserAccountFixture.ash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RegisterUserInteractorTest {

    private static final PasswordHash HASH = new PasswordHash("$2a$10$hashedpikachu1");

    @Mock private UserAccountRepository repository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private UnitOfWork unitOfWork;

    private RegisterUserInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new RegisterUserInteractor(repository, passwordHasher, new UserAccountMapper(), unitOfWork);
        lenient().when(unitOfWork.inTransaction(ArgumentMatchers.<Supplier<Object>>any()))
            .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
    }

    @Test
    void shouldRegisterAnAccountWithTheGivenIdAndAHashedPassword() {
        given(passwordHasher.hash(new RawPassword("pikachu123"))).willReturn(HASH);
        given(repository.save(any(UserAccount.class))).willAnswer(invocation -> invocation.getArgument(0));

        var output = interactor.execute(
            new RegisterUserInput(" Ash@Pallet.town ", "Ash Ketchum", "pikachu123"), ASH_ID, NOW);

        assertThat(output.id()).isEqualTo(ASH_ID.value());
        assertThat(output.email()).isEqualTo("ash@pallet.town");
        assertThat(output.name()).isEqualTo("Ash Ketchum");
        assertThat(output.createdAt()).isEqualTo(NOW);
        then(repository).should().save(argThat(saved -> saved.getPasswordHash().equals(HASH)));
    }

    @Test
    void shouldRefuseAnEmailThatIsAlreadyRegistered() {
        given(passwordHasher.hash(any())).willReturn(HASH);
        given(repository.findByEmail(new Email("ash@pallet.town"))).willReturn(Optional.of(ash()));

        assertThatThrownBy(() -> interactor.execute(
                new RegisterUserInput("ash@pallet.town", "Another Ash", "pikachu123"), ASH_ID, NOW))
            .isInstanceOf(EmailAlreadyRegisteredException.class);

        then(repository).should(never()).save(any());
    }

    @Test
    void shouldRejectAWeakPasswordBeforeHashingOrTouchingTheDatabase() {
        assertThatThrownBy(() -> interactor.execute(
                new RegisterUserInput("ash@pallet.town", "Ash Ketchum", "pikachu"), ASH_ID, NOW))
            .isInstanceOf(WeakPasswordException.class);

        verifyNoInteractions(passwordHasher, repository, unitOfWork);
    }
}
