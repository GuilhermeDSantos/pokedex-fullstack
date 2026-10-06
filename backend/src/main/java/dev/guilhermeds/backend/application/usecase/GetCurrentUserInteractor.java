package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetCurrentUserInput;
import dev.guilhermeds.backend.application.dto.UserOutput;
import dev.guilhermeds.backend.application.mapper.UserAccountMapper;
import dev.guilhermeds.backend.domain.exception.UnknownAccountException;
import dev.guilhermeds.backend.domain.repository.UserAccountRepository;

public class GetCurrentUserInteractor implements GetCurrentUserUseCase {

    private final UserAccountRepository repository;
    private final UserAccountMapper mapper;

    public GetCurrentUserInteractor(UserAccountRepository repository, UserAccountMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserOutput execute(GetCurrentUserInput input) {
        throw new UnknownAccountException();
    }
}
