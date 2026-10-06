package dev.guilhermeds.backend.application.usecase;

import dev.guilhermeds.backend.application.dto.AccessTokenOutput;
import dev.guilhermeds.backend.application.dto.AuthenticateUserInput;

import java.time.Instant;

public interface AuthenticateUserUseCase {
    AccessTokenOutput execute(AuthenticateUserInput input, Instant now);
}
