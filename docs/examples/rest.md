# REST Layer (`interfaces/rest`) — Examples

Reference code for the delivery layer, against the contract in
[`../domain-model.md#api-contract`](../domain-model.md#api-contract). Rules:
[`../standards/backend.md`](../standards/backend.md).

## Base path

Every controller maps under the plain prefix `/api/v1` (D-029). The `v1` is a label for the
published contract, not framework versioning. Native API versioning is parked: a breaking change
would get a new `/api/v2` controller next to the old one.

## Controller

Thin. It takes a `@Valid` request, calls **one input port**, maps the result, and returns it. No
logic, no try/catch, and never an `*Interactor`.

It's also **the edge**: the only layer that reads the clock (`Instant.now(clock)`, once per
request) and mints new ids (`LocalPokemonId.generate()`). Neither can come from a request body.

One controller for one resource. The reads are the merged view, and the writes go to the `/local`
sub-resource, so every verb means exactly what it says (D-030).

```java
// interfaces/rest/controller/PokemonController.java
@RestController
@RequestMapping("/api/v1/pokemon")
public class PokemonController {

    private final BrowsePokemonUseCase browsePokemonUseCase;          // input ports — interfaces
    private final GetPokemonUseCase getPokemonUseCase;
    private final GetLocalPokemonUseCase getLocalPokemonUseCase;
    private final SyncPokemonUseCase syncPokemonUseCase;
    private final UpdateLocalPokemonUseCase updateLocalPokemonUseCase;
    private final RemoveLocalPokemonUseCase removeLocalPokemonUseCase;
    private final PokemonRestMapper mapper;
    private final Clock clock;

    // constructor omitted — constructor injection only

    // ---- merged reads (public) ---------------------------------------------------

    @GetMapping
    public PageResponse<PokemonSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return mapper.toPageResponse(browsePokemonUseCase.execute(new BrowsePokemonInput(page, size)));
    }

    @GetMapping("/{identifier}")
    public PokemonDetailResponse get(@PathVariable String identifier) {
        return mapper.toResponse(getPokemonUseCase.execute(new GetPokemonInput(identifier)));
    }

    // ---- the local record: /local sub-resource -----------------------------------

    @GetMapping("/{identifier}/local")
    public LocalPokemonResponse getLocal(@PathVariable String identifier) {
        return mapper.toResponse(getLocalPokemonUseCase.execute(new GetLocalPokemonInput(identifier)));
    }

    @PostMapping("/{identifier}/local")
    public ResponseEntity<LocalPokemonResponse> sync(@PathVariable String identifier) {
        // Both ambient values originate here, once, and are passed down explicitly.
        var output = syncPokemonUseCase.execute(
            new SyncPokemonInput(identifier), LocalPokemonId.generate(), Instant.now(clock));
        // Location uses the canonical number, whatever the client sent (name or number).
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/pokemon/{number}/local").buildAndExpand(output.pokedexNumber()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(output));
    }

    @PutMapping("/{identifier}/local")
    public LocalPokemonResponse update(@PathVariable String identifier,
                                       @RequestBody @Valid UpdateLocalPokemonRequest request) {
        return mapper.toResponse(updateLocalPokemonUseCase.execute(mapper.toInput(identifier, request), Instant.now(clock)));
    }

    @DeleteMapping("/{identifier}/local")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String identifier) {
        removeLocalPokemonUseCase.execute(new RemoveLocalPokemonInput(identifier));
    }
}
```

`LocalPokemonId.generate()` here is the *only* call site (checked by review). Building
`new GetPokemonInput(identifier)` in the controller is fine: it's an application DTO made of raw
strings, not a domain object. Turning it into a `PokemonIdentifier` (and the 400 for a malformed
one) happens in the application mapper.

## Request bodies (syntactic validation)

**The domain is the validation authority (D-028).** Bean Validation here checks **shape** only:
required fields and collection/string sizes, for fast field-specific 400s. Every limit references
the domain constant instead of repeating a literal, so the two layers can't drift. **No format rule
lives only at the edge.** There's no `@Email` and no pattern for tags. The `Email` and `Tag` VOs
decide, and their `ValidationException` is a 400 too.

```java
// interfaces/rest/request/UpdateLocalPokemonRequest.java
public record UpdateLocalPokemonRequest(
    @Size(max = CustomAttributes.MAX_TEXT_LENGTH) String localizedName,
    @Size(max = CustomAttributes.MAX_TEXT_LENGTH) String region,
    @Size(max = CustomAttributes.MAX_TAGS) List<@NotBlank String> tags   // format: the Tag VO decides
) {}

// interfaces/rest/request/RegisterUserRequest.java — required-ness only; Email, FullName and
// RawPassword own format, length and the 72-byte BCrypt limit.
public record RegisterUserRequest(
    @NotBlank String email,
    @NotBlank String name,
    @NotBlank String password
) {}
```

Sync has no body: the identifier in the path is all it needs.

## REST mapper

`{Name}RestMapper` (`@Component`) converts HTTP ↔ application DTOs and never touches domain
objects. The HTTP contract can evolve (renames, a future `/api/v2` shape) without the application
layer noticing.

```java
// interfaces/rest/mapper/PokemonRestMapper.java
@Component
public class PokemonRestMapper {

    public UpdateLocalPokemonInput toInput(String identifier, UpdateLocalPokemonRequest request) {
        return new UpdateLocalPokemonInput(identifier, request.localizedName(), request.region(), request.tags());
    }

    public LocalPokemonResponse toResponse(LocalPokemonOutput output) {
        return new LocalPokemonResponse(output.pokedexNumber(), output.name(), output.displayName(),
            output.localizedName(), output.region(), output.tags(), output.syncedAt(), output.updatedAt());
    }

    public PokemonDetailResponse toResponse(PokemonDetailOutput output) { /* field by field; local may be null */ }

    public PageResponse<PokemonSummaryResponse> toPageResponse(PageOutput<PokemonSummaryOutput> page) {
        return new PageResponse<>(page.content().stream().map(this::toResponse).toList(),
            page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
```

## Uniform response shapes

```java
// interfaces/rest/response/ErrorResponse.java
public record ErrorResponse(String code, String message, List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }
}

// interfaces/rest/response/PageResponse.java
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
```

## Global exception handler

One class. Domain exceptions are mapped **by category only**, never by concrete class, so a new
`*NotFoundException` gets 404 and `NOT_FOUND` without touching this file. Framework exceptions go
into the same `ErrorResponse` shape, which is what "consistent return structures" means in
practice.

```java
// interfaces/rest/controller/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- domain categories ---------------------------------------------------

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleDomainValidation(ValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException ex) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", ex.getMessage());
    }

    /** Catch-all for one-off business rule violations with no category. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "DOMAIN_ERROR", ex.getMessage());
    }

    // ---- PokeAPI port contract -------------------------------------------------

    @ExceptionHandler(PokemonSourceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleSourceUnavailable(PokemonSourceUnavailableException ex) {
        log.warn("PokeAPI unavailable: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", ex.getMessage());
    }

    // ---- request-level (syntactic) problems → 400 -----------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", "Request body is invalid", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
        // Never echo the parser's message — it can contain the raw payload.
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Malformed JSON request body");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
                       HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> handleBadParameter(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameter");
    }

    // ---- last resort ---------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Without this check, Spring's own 405/415/... would be swallowed into a 500.
        if (ex instanceof org.springframework.web.ErrorResponse frameworkError) {
            var status = HttpStatus.valueOf(frameworkError.getStatusCode().value());
            return error(status, status.name(), status.getReasonPhrase());
        }
        log.error("Unexpected error", ex);  // full stack trace in the log…
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred"); // …never in the body
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }
}
```

There's also `NoResourceFoundException` → 404 (for unknown paths), and Spring Security's 401/403,
written in the same `ErrorResponse` shape by the two classes below. The frontend then only ever has
to parse one error format.

## Security error writers

Spring Security rejects a request before any controller runs, so `GlobalExceptionHandler` never
sees it. These two classes write the same `ErrorResponse` instead. They live in
`interfaces/rest/security/`, next to `ErrorResponse`, because the HTTP error shape is a delivery
concern. `SecurityConfig` (infrastructure) receives them through Spring Security's own interfaces,
so it never imports anything from `interfaces` (ArchUnit:
`infrastructure_must_not_depend_on_interfaces`).

```java
// interfaces/rest/security/ErrorResponseAuthenticationEntryPoint.java — 401
@Component
public class ErrorResponseAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;   // Jackson 3 (tools.jackson.databind.json) — verify the Boot 4.1 bean

    public ErrorResponseAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Never echo authException's message: it can describe why a token was rejected.
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(),
            ErrorResponse.of("UNAUTHENTICATED", "Authentication is required to access this resource"));
    }
}

// interfaces/rest/security/ErrorResponseAccessDeniedHandler.java — 403
// Same shape: implements AccessDeniedHandler, status 403, code "FORBIDDEN". No roles exist yet
// (D-030), so it's the safety net for when they do.
```

`@WebMvcTest` doesn't scan plain `@Component`s, so controller ITs `@Import` both classes along with
`SecurityConfig`.
