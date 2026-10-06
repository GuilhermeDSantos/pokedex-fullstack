package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.application.usecase.RegisterUserUseCase;
import dev.guilhermeds.backend.domain.model.UserId;
import dev.guilhermeds.backend.interfaces.rest.mapper.AuthRestMapper;
import dev.guilhermeds.backend.interfaces.rest.request.RegisterUserRequest;
import dev.guilhermeds.backend.interfaces.rest.response.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthRestMapper mapper;
    private final Clock clock;

    public AuthController(RegisterUserUseCase registerUserUseCase, AuthRestMapper mapper, Clock clock) {
        this.registerUserUseCase = registerUserUseCase;
        this.mapper = mapper;
        this.clock = clock;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody RegisterUserRequest request) {
        return mapper.toResponse(
            registerUserUseCase.execute(mapper.toInput(request), UserId.generate(), Instant.now(clock)));
    }
}
