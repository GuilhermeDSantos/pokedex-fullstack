package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.domain.exception.ConflictException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import dev.guilhermeds.backend.domain.exception.UnauthenticatedException;
import dev.guilhermeds.backend.domain.exception.DomainException;
import dev.guilhermeds.backend.domain.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The exceptions below exist only in this test: the handler has never heard of them, and still maps
 * each one by its category. That's the property under test.
 */
@WebMvcTest(GlobalExceptionHandlerIT.ProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerIT.ProbeController.class) // nested test classes aren't component-scanned
class GlobalExceptionHandlerIT {

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void shouldMapAnyNotFoundCategoryTo404() {
        assertThat(mockMvc.get().uri("/probe/not-found"))
            .hasStatus(404)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "NOT_FOUND", "message": "Probe 42 not found", "fieldErrors": [] }
                """);
    }

    @Test
    void shouldMapAnyConflictCategoryTo409() {
        assertThat(mockMvc.get().uri("/probe/conflict"))
            .hasStatus(409)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "CONFLICT", "message": "Probe 42 already exists", "fieldErrors": [] }
                """);
    }

    @Test
    void shouldMapAnyValidationCategoryTo400() {
        assertThat(mockMvc.get().uri("/probe/invalid"))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "VALIDATION_ERROR", "message": "Probe name is invalid", "fieldErrors": [] }
                """);
    }

    @Test
    void shouldMapAnyUnauthenticatedCategoryTo401() {
        assertThat(mockMvc.get().uri("/probe/unauthenticated"))
            .hasStatus(401)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "UNAUTHENTICATED", "message": "Probe credentials are invalid", "fieldErrors": [] }
                """);
    }

    @Test
    void shouldMapAnyOtherDomainExceptionTo422() {
        assertThat(mockMvc.get().uri("/probe/rule-broken"))
            .hasStatus(422)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "DOMAIN_ERROR", "message": "Probe 42 cannot do that", "fieldErrors": [] }
                """);
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/rule-broken")
        void rule_broken() {
            throw new ProbeRuleBrokenException();
        }

        @GetMapping("/probe/unauthenticated")
        void unauthenticated() {
            throw new ProbeUnauthenticatedException();
        }

        @GetMapping("/probe/invalid")
        void invalid() {
            throw new ProbeInvalidException();
        }

        @GetMapping("/probe/conflict")
        void conflict() {
            throw new ProbeConflictException();
        }

        @GetMapping("/probe/not-found")
        void notFound() {
            throw new ProbeNotFoundException();
        }
    }

    static class ProbeNotFoundException extends NotFoundException {
        ProbeNotFoundException() {
            super("Probe 42 not found");
        }
    }

    static class ProbeConflictException extends ConflictException {
        ProbeConflictException() {
            super("Probe 42 already exists");
        }
    }

    static class ProbeInvalidException extends ValidationException {
        ProbeInvalidException() {
            super("Probe name is invalid");
        }
    }

    static class ProbeUnauthenticatedException extends UnauthenticatedException {
        ProbeUnauthenticatedException() {
            super("Probe credentials are invalid");
        }
    }

    static class ProbeRuleBrokenException extends DomainException {
        ProbeRuleBrokenException() {
            super("Probe 42 cannot do that");
        }
    }
}
