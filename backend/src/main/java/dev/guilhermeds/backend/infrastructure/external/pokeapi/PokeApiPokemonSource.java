package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.source.PokemonSource;
import dev.guilhermeds.backend.domain.source.PokemonSourceUnavailableException;
import dev.guilhermeds.backend.domain.source.PokemonSummary;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// The PokemonSource port on PokeAPI. No @Cacheable here: caching lives on the client (D-012).
@Component
public class PokeApiPokemonSource implements PokemonSource {

    private final PokeApiClient client;
    private final PokeApiTranslator translator;

    public PokeApiPokemonSource(PokeApiClient client, PokeApiTranslator translator) {
        this.client = client;
        this.translator = translator;
    }

    @Override
    public Page<PokemonSummary> findAll(PageRequest pageRequest) {
        var page = client.fetchPage(pageRequest.offset(), pageRequest.size());
        // Two calls per card: fetched concurrently on virtual threads, joined in PokeAPI's order (D-018).
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var cards = page.results().stream()
                .map(entry -> executor.submit(() -> summaryOf(entry.name())))
                .toList();
            return new Page<>(cards.stream().map(PokeApiPokemonSource::join).toList(), page.count());
        }
    }

    // The list only names each Pokémon; its card needs the Pokémon and its species.
    private PokemonSummary summaryOf(String name) {
        var pokemon = client.fetchPokemon(name)
            .orElseThrow(() -> new PokemonSourceUnavailableException("PokeAPI listed a Pokémon it can't return: " + name));
        return translator.toSummary(pokemon, client.fetchSpecies(pokemon.species().url()));
    }

    private static PokemonSummary join(Future<PokemonSummary> card) {
        try {
            return card.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException failure) {
                throw failure;
            }
            throw new PokemonSourceUnavailableException("PokeAPI call failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PokemonSourceUnavailableException("Interrupted while calling PokeAPI", e);
        }
    }
}
