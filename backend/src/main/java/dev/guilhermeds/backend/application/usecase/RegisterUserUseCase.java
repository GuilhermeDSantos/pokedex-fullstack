package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.RegisterUserInput;
import dev.guilhermeds.backend.application.dto.UserOutput;
import dev.guilhermeds.backend.domain.model.UserId;

import java.time.Instant;

public interface RegisterUserUseCase {
    UserOutput execute(RegisterUserInput input, UserId id, Instant now);
}
