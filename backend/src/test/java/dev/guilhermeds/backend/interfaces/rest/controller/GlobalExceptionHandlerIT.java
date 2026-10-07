package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.domain.exception.ConflictException;
import dev.guilhermeds.backend.domain.exception.DataUnavailableException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import dev.guilhermeds.backend.domain.exception.UnauthenticatedException;
import dev.guilhermeds.backend.domain.exception.DomainException;
import dev.guilhermeds.backend.domain.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

    // Whatever store is down, the client gets the same neutral 503; which one, and why, is in the log.
    @Test
    void shouldMapAnyDataUnavailableCategoryTo503WithoutItsDetails() {
        var result = mockMvc.get().uri("/probe/data-unavailable").exchange();

        assertThat(result)
            .hasStatus(503)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "DATA_UNAVAILABLE",
                  "message": "The service is temporarily unavailable. Please try again in a moment.",
                  "fieldErrors": [] }
                """);
        assertThat(result).bodyText().doesNotContain("probe-store");
    }

    @Test
    void shouldReturn400WithoutEchoingTheBodyWhenJsonIsMalformed() {
        assertThat(mockMvc.post().uri("/probe/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"name\": "))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "VALIDATION_ERROR", "message": "Malformed JSON request body", "fieldErrors": [] }
                """);
    }

    @Test
    void shouldReturn400WithFieldErrorsWhenTheBodyIsInvalid() {
        assertThat(mockMvc.post().uri("/probe/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"name\": \" \" }"))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "VALIDATION_ERROR", "message": "Request body is invalid",
                  "fieldErrors": [ { "field": "name", "message": "must not be blank" } ] }
                """);
    }

    @Test
    void shouldKeepValidationMessagesInEnglishWhateverTheBrowserLanguage() {
        assertThat(mockMvc.post().uri("/probe/valid")
                .header("Accept-Language", "pt-BR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"name\": \" \" }"))
            .hasStatus(400)
            .bodyJson().extractingPath("$.fieldErrors[0].message").isEqualTo("must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/probe/params", "/probe/params?page=abc"})
    void shouldReturn400WhenAParameterIsMissingOrOfTheWrongType(String uri) {
        assertThat(mockMvc.get().uri(uri))
            .hasStatus(400)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "VALIDATION_ERROR", "message": "Invalid request parameter", "fieldErrors": [] }
                """);
    }

    @Test
    void shouldReturn404ForAnUnknownPath() {
        assertThat(mockMvc.get().uri("/probe/does-not-exist"))
            .hasStatus(404)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "NOT_FOUND", "message": "Resource not found", "fieldErrors": [] }
                """);
    }

    @Test
    void shouldReturnAGeneric500WithoutLeakingTheCause() {
        var result = mockMvc.get().uri("/probe/unexpected").exchange();

        assertThat(result)
            .hasStatus(500)
            .bodyJson()
            .isLenientlyEqualTo("""
                { "code": "INTERNAL_ERROR", "message": "An unexpected error occurred", "fieldErrors": [] }
                """);
        assertThat(result).bodyText().doesNotContain("s3cr3t");
    }

    @ParameterizedTest
    @CsvSource({
        "GET,  application/json, 405, METHOD_NOT_ALLOWED",
        "POST, text/plain,       415, UNSUPPORTED_MEDIA_TYPE"
    })
    void shouldKeepTheStatusOfFrameworkErrors(String method, String contentType, int status, String code) {
        assertThat(mockMvc.method(HttpMethod.valueOf(method)).uri("/probe/body")
                .contentType(contentType)
                .content("{}"))
            .hasStatus(status)
            .bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/unexpected")
        void unexpected() {
            throw new IllegalStateException("connection string with s3cr3t");
        }

        @GetMapping("/probe/params")
        void params(@RequestParam int page) {
        }

        @PostMapping("/probe/valid")
        void valid(@Valid @RequestBody ProbeValidRequest request) {
        }

        @PostMapping("/probe/body")
        void body(@RequestBody ProbeRequest request) {
        }

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

        @GetMapping("/probe/data-unavailable")
        void dataUnavailable() {
            throw new ProbeDataUnavailableException();
        }

        @GetMapping("/probe/not-found")
        void notFound() {
            throw new ProbeNotFoundException();
        }
    }

    static class ProbeDataUnavailableException extends DataUnavailableException {
        ProbeDataUnavailableException() {
            super("probe-store refused the connection", new IllegalStateException("connection refused"));
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

    record ProbeRequest(String name) {
    }

    record ProbeValidRequest(@NotBlank String name) {
    }
}
