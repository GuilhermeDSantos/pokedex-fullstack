package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.application.dto.RegisterUserInput;
import dev.guilhermeds.backend.application.dto.UserOutput;
import dev.guilhermeds.backend.application.usecase.AuthenticateUserUseCase;
import dev.guilhermeds.backend.application.usecase.GetCurrentUserUseCase;
import dev.guilhermeds.backend.application.usecase.RegisterUserUseCase;
import dev.guilhermeds.backend.infrastructure.config.JwtConfig;
import dev.guilhermeds.backend.infrastructure.config.SecurityConfig;
import dev.guilhermeds.backend.interfaces.rest.mapper.AuthRestMapper;
import dev.guilhermeds.backend.interfaces.rest.security.ErrorResponseAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Clock;
import java.time.ZoneOffset;

import static dev.guilhermeds.backend.fixture.UserAccountFixture.ASH_ID;
import static dev.guilhermeds.backend.fixture.UserAccountFixture.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(AuthController.class)
@Import({AuthRestMapper.class, SecurityConfig.class, JwtConfig.class, ErrorResponseAuthenticationEntryPoint.class,
    AuthControllerIT.FixedClock.class})
class AuthControllerIT {

    private static final UserOutput ASH = new UserOutput(ASH_ID.value(), "ash@pallet.town", "Ash Ketchum", NOW);

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Test
    void shouldRegisterAUserAndReturnIt() {
        given(registerUserUseCase.execute(
                eq(new RegisterUserInput("ash@pallet.town", "Ash Ketchum", "pikachu123")), any(), eq(NOW)))
            .willReturn(ASH);

        assertThat(mockMvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "ash@pallet.town", "name": "Ash Ketchum", "password": "pikachu123" }
                    """))
            .hasStatus(201)
            .bodyJson()
            .isStrictlyEqualTo("""
                { "id": "00000000-0000-0000-0000-000000000001", "email": "ash@pallet.town",
                  "name": "Ash Ketchum", "createdAt": "2026-01-15T10:00:00Z" }
                """);
    }

    @Test
    void shouldRejectARegistrationWithMissingFields() {
        assertThat(mockMvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": " ", "name": "Ash Ketchum" }
                    """))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "VALIDATION_ERROR", "message": "Request body is invalid",
                  "fieldErrors": [ { "field": "email", "message": "must not be blank" },
                                   { "field": "password", "message": "must not be blank" } ] }
                """);
        verifyNoInteractions(registerUserUseCase);
    }

    @Test
    void shouldRejectAMalformedRegistrationBody() {
        assertThat(mockMvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": "))
            .hasStatus(400)
            .bodyJson().extractingPath("$.message").isEqualTo("Malformed JSON request body");
    }
}
