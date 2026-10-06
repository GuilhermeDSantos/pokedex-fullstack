package dev.guilhermeds.backend.interfaces.rest.request;

public record AuthenticateUserRequest(String email, String password) {
}
