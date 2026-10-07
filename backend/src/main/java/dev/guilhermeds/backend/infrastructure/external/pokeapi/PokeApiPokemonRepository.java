package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.PokemonIdentifier;
import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;
import dev.guilhermeds.backend.domain.model.PokemonDetail;
import dev.guilhermeds.backend.domain.repository.PokemonRepository;
import dev.guilhermeds.backend.domain.exception.PokemonDataUnavailableException;
import dev.guilhermeds.backend.domain.model.PokemonSummary;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

// The PokemonRepository port on PokeAPI. No @Cacheable here: caching lives on the client (D-012).
@Component
public class PokeApiPokemonRepository implements PokemonRepository {

    private final PokeApiClient client;
    private final PokeApiTranslator translator;
    // Shared by every request, so the cap holds across concurrent users too.
    private final Semaphore pokeApiCalls;

    public PokeApiPokemonRepository(PokeApiClient client, PokeApiTranslator translator, PokeApiProperties properties) {
        this.client = client;
        this.translator = translator;
        this.pokeApiCalls = new Semaphore(properties.maxConcurrency());
    }

    @Override
    public Page<PokemonSummary> findAll(PageRequest pageRequest) {
        var page = client.fetchPage(pageRequest.offset(), pageRequest.size());
        // Two calls per card: fetched concurrently on virtual threads, joined in PokeAPI's order (D-018).
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var cards = page.results().stream()
                .map(entry -> executor.submit(() -> withinTheCap(() -> summaryOf(entry.name()))))
                .toList();
            return new Page<>(cards.stream().map(PokeApiPokemonRepository::join).toList(), page.count());
        }
    }

    // No @Cacheable here either: getByIdentifier calls this on `this`, past any proxy; the client caches.
    @Override
    public Optional<PokemonDetail> findByIdentifier(PokemonIdentifier identifier) {
        return client.fetchPokemon(identifier.value()).map(pokemon -> {
            var species = client.fetchSpecies(pokemon.species().url());
            var chain = client.fetchEvolutionChain(species.evolutionChain().url());
            return translator.toDetail(pokemon, species, chain);
        });
    }

    private PokemonSummary withinTheCap(Supplier<PokemonSummary> call) throws InterruptedException {
        pokeApiCalls.acquire();
        try {
            return call.get();
        } finally {
            pokeApiCalls.release();
        }
    }

    // The list only names each Pokémon; its card needs the Pokémon and its species.
    private PokemonSummary summaryOf(String name) {
        var pokemon = client.fetchPokemon(name)
            .orElseThrow(() -> new PokemonDataUnavailableException("PokeAPI listed a Pokémon it can't return: " + name));
        return translator.toSummary(pokemon, client.fetchSpecies(pokemon.species().url()));
    }

    private static PokemonSummary join(Future<PokemonSummary> card) {
        try {
            return card.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException failure) {
                throw failure;
            }
            throw new PokemonDataUnavailableException("PokeAPI call failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PokemonDataUnavailableException("Interrupted while calling PokeAPI", e);
        }
    }
}
