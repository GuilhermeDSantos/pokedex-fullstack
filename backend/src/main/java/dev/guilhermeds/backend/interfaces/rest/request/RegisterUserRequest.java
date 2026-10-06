package dev.guilhermeds.backend.interfaces.rest.request;

public record RegisterUserRequest(String email, String name, String password) {
}
