package dev.guilhermeds.backend.interfaces.rest.controller;

import dev.guilhermeds.backend.application.dto.BrowsePokemonInput;
import dev.guilhermeds.backend.application.usecase.BrowsePokemonUseCase;
import dev.guilhermeds.backend.interfaces.rest.mapper.PokemonRestMapper;
import dev.guilhermeds.backend.interfaces.rest.response.PageResponse;
import dev.guilhermeds.backend.interfaces.rest.response.PokemonSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pokemon")
public class PokemonController {

    private final BrowsePokemonUseCase browsePokemonUseCase;
    private final PokemonRestMapper mapper;

    public PokemonController(BrowsePokemonUseCase browsePokemonUseCase, PokemonRestMapper mapper) {
        this.browsePokemonUseCase = browsePokemonUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public PageResponse<PokemonSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return mapper.toPageResponse(browsePokemonUseCase.execute(new BrowsePokemonInput(page, size)));
    }
}
