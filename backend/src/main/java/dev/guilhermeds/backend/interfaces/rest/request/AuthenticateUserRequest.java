package dev.guilhermeds.backend.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record AuthenticateUserRequest(
    @NotBlank String email,
    @NotBlank String password
) {
}
