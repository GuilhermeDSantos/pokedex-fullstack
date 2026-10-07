package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.application.dto.GetLocalPokemonInput;
import dev.guilhermeds.backend.application.dto.GetPokemonInput;
import dev.guilhermeds.backend.application.dto.SyncPokemonInput;
import dev.guilhermeds.backend.application.usecase.BrowsePokemonUseCase;
import dev.guilhermeds.backend.application.usecase.GetLocalPokemonUseCase;
import dev.guilhermeds.backend.application.usecase.GetPokemonUseCase;
import dev.guilhermeds.backend.application.usecase.SyncPokemonUseCase;
import dev.guilhermeds.backend.domain.model.LocalPokemonId;
import dev.guilhermeds.backend.interfaces.rest.OpenApiDocumentation;
import dev.guilhermeds.backend.interfaces.rest.mapper.PokemonRestMapper;
import dev.guilhermeds.backend.interfaces.rest.response.LocalPokemonResponse;
import dev.guilhermeds.backend.interfaces.rest.response.PageResponse;
import dev.guilhermeds.backend.interfaces.rest.response.PokemonDetailResponse;
import dev.guilhermeds.backend.interfaces.rest.response.PokemonSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/pokemon")
public class PokemonController {

    private final BrowsePokemonUseCase browsePokemonUseCase;
    private final GetPokemonUseCase getPokemonUseCase;
    private final SyncPokemonUseCase syncPokemonUseCase;
    private final GetLocalPokemonUseCase getLocalPokemonUseCase;
    private final PokemonRestMapper mapper;
    private final Clock clock;

    public PokemonController(BrowsePokemonUseCase browsePokemonUseCase, GetPokemonUseCase getPokemonUseCase,
                             SyncPokemonUseCase syncPokemonUseCase, GetLocalPokemonUseCase getLocalPokemonUseCase,
                             PokemonRestMapper mapper, Clock clock) {
        this.browsePokemonUseCase = browsePokemonUseCase;
        this.getPokemonUseCase = getPokemonUseCase;
        this.syncPokemonUseCase = syncPokemonUseCase;
        this.getLocalPokemonUseCase = getLocalPokemonUseCase;
        this.mapper = mapper;
        this.clock = clock;
    }

    @GetMapping
    public PageResponse<PokemonSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return mapper.toPageResponse(browsePokemonUseCase.execute(new BrowsePokemonInput(page, size)));
    }

    @GetMapping("/{identifier}")
    public PokemonDetailResponse get(@PathVariable String identifier) {
        return mapper.toResponse(getPokemonUseCase.execute(new GetPokemonInput(identifier)));
    }

    @PostMapping("/{number}/local")
    @SecurityRequirement(name = OpenApiDocumentation.BEARER_JWT)
    public ResponseEntity<LocalPokemonResponse> sync(@PathVariable String number) {
        var synced = syncPokemonUseCase.execute(new SyncPokemonInput(number), LocalPokemonId.generate(),
            Instant.now(clock));
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/pokemon/{number}/local").buildAndExpand(synced.pokedexNumber()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(synced));
    }

    @GetMapping("/{number}/local")
    public LocalPokemonResponse getLocal(@PathVariable String number) {
        return mapper.toResponse(getLocalPokemonUseCase.execute(new GetLocalPokemonInput(number)));
    }
}
