package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.domain.exception.ConflictException;
import dev.guilhermeds.backend.domain.exception.DataUnavailableException;
import dev.guilhermeds.backend.domain.exception.DomainException;
import dev.guilhermeds.backend.domain.exception.NotFoundException;
import dev.guilhermeds.backend.domain.exception.UnauthenticatedException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import dev.guilhermeds.backend.interfaces.rest.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
        return error(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException exception) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", exception.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException exception) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "DOMAIN_ERROR", exception.getMessage());
    }

    // The message names the store and the cause: for the log, where the root cause is read, never the client.
    @ExceptionHandler(DataUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleDataUnavailable(DataUnavailableException exception) {
        log.warn("Data unavailable ({})", exception.getClass().getSimpleName(), exception);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "DATA_UNAVAILABLE",
            "The service is temporarily unavailable. Please try again in a moment.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException exception) {
        var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", "Request body is invalid", fieldErrors));
    }

    // The parser's message can contain the raw payload, so it's never echoed.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Malformed JSON request body");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleInvalidParameter() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameter");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnknownPath() {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        // Spring's own web exceptions (405, 415, ...) already know their status; only the rest is a 500.
        if (exception instanceof org.springframework.web.ErrorResponse frameworkError) {
            var status = HttpStatus.valueOf(frameworkError.getStatusCode().value());
            return error(status, status.name(), status.getReasonPhrase());
        }
        log.error("Unexpected error", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }
}
