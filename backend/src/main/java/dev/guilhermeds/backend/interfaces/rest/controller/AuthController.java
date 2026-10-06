package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.application.usecase.AuthenticateUserUseCase;
import dev.guilhermeds.backend.application.usecase.GetCurrentUserUseCase;
import dev.guilhermeds.backend.application.usecase.RegisterUserUseCase;
import dev.guilhermeds.backend.domain.model.UserId;
import dev.guilhermeds.backend.interfaces.rest.OpenApiDocumentation;
import dev.guilhermeds.backend.interfaces.rest.mapper.AuthRestMapper;
import dev.guilhermeds.backend.interfaces.rest.request.AuthenticateUserRequest;
import dev.guilhermeds.backend.interfaces.rest.request.RegisterUserRequest;
import dev.guilhermeds.backend.interfaces.rest.response.AccessTokenResponse;
import dev.guilhermeds.backend.interfaces.rest.response.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final AuthRestMapper mapper;
    private final Clock clock;

    public AuthController(RegisterUserUseCase registerUserUseCase, AuthenticateUserUseCase authenticateUserUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase, AuthRestMapper mapper, Clock clock) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.mapper = mapper;
        this.clock = clock;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody @Valid RegisterUserRequest request) {
        return mapper.toResponse(
            registerUserUseCase.execute(mapper.toInput(request), UserId.generate(), Instant.now(clock)));
    }

    @PostMapping("/login")
    public AccessTokenResponse login(@RequestBody @Valid AuthenticateUserRequest request) {
        return mapper.toResponse(authenticateUserUseCase.execute(mapper.toInput(request), Instant.now(clock)));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = OpenApiDocumentation.BEARER_JWT)
    public UserResponse me(@AuthenticationPrincipal Jwt token) {
        return mapper.toResponse(getCurrentUserUseCase.execute(mapper.toInput(token)));
    }
}
