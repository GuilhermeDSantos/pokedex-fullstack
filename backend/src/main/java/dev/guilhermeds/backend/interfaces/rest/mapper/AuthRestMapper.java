package dev.guilhermeds.backend.interfaces.rest.mapper;

import dev.guilhermeds.backend.application.dto.AccessTokenOutput;
import dev.guilhermeds.backend.application.dto.AuthenticateUserInput;
import dev.guilhermeds.backend.application.dto.RegisterUserInput;
import dev.guilhermeds.backend.application.dto.UserOutput;
import dev.guilhermeds.backend.interfaces.rest.request.AuthenticateUserRequest;
import dev.guilhermeds.backend.interfaces.rest.request.RegisterUserRequest;
import dev.guilhermeds.backend.interfaces.rest.response.AccessTokenResponse;
import dev.guilhermeds.backend.interfaces.rest.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthRestMapper {

    private static final String BEARER = "Bearer";

    public RegisterUserInput toInput(RegisterUserRequest request) {
        return new RegisterUserInput(request.email(), request.name(), request.password());
    }

    public UserResponse toResponse(UserOutput output) {
        return new UserResponse(output.id(), output.email(), output.name(), output.createdAt());
    }

    public AuthenticateUserInput toInput(AuthenticateUserRequest request) {
        return new AuthenticateUserInput(request.email(), request.password());
    }

    public AccessTokenResponse toResponse(AccessTokenOutput output) {
        return new AccessTokenResponse(output.accessToken(), BEARER, output.expiresAt());
    }
}
