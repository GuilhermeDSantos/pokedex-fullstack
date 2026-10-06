package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.domain.exception.ConflictException;
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

    @RestController
    static class ProbeController {

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
}
