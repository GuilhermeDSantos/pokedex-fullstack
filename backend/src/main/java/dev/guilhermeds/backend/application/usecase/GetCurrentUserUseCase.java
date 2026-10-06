package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.GetCurrentUserInput;
import dev.guilhermeds.backend.application.dto.UserOutput;

public interface GetCurrentUserUseCase {
    UserOutput execute(GetCurrentUserInput input);
}
