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

import java.util.Optional;

import static dev.guilhermeds.backend.fixture.UserAccountFixture.ASH_ID;
import static dev.guilhermeds.backend.fixture.UserAccountFixture.NOW;
import static dev.guilhermeds.backend.fixture.UserAccountFixture.ash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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

    @Test
    void shouldReturnTheAccountTheTokenBelongsTo() {
        given(repository.findById(ASH_ID)).willReturn(Optional.of(ash()));

        var output = interactor.execute(new GetCurrentUserInput(ASH_ID.value()));

        assertThat(output.id()).isEqualTo(ASH_ID.value());
        assertThat(output.email()).isEqualTo("ash@pallet.town");
        assertThat(output.name()).isEqualTo("Ash Ketchum");
        assertThat(output.createdAt()).isEqualTo(NOW);
    }
}
