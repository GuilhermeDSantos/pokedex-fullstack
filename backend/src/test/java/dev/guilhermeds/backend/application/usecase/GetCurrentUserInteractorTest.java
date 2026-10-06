package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetCurrentUserInput;
import dev.guilhermeds.backend.application.mapper.UserAccountMapper;
import dev.guilhermeds.backend.domain.exception.UnknownAccountException;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static dev.guilhermeds.backend.fixture.UserAccountFixture.ASH_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserInteractorTest {

    @Mock private UserAccountRepository repository;

    private GetCurrentUserInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new GetCurrentUserInteractor(repository, new UserAccountMapper());
    }

    @Test
    void shouldRefuseATokenWhoseAccountNoLongerExists() {
        assertThatThrownBy(() -> interactor.execute(new GetCurrentUserInput(ASH_ID.value())))
            .isInstanceOf(UnknownAccountException.class)
            .hasMessage("Your session is no longer valid, please sign in again");
    }
}
