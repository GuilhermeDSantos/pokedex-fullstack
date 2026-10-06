package dev.guilhermeds.backend.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

// Required-ness only: Email, FullName and RawPassword own format and length (D-028).
public record RegisterUserRequest(
    @NotBlank String email,
    @NotBlank String name,
    @NotBlank String password
) {
}
