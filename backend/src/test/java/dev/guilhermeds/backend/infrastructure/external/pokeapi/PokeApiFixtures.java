package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

// Responses recorded from pokeapi.co with curl (src/test/resources/pokeapi), so tests never hit the network.
final class PokeApiFixtures {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private PokeApiFixtures() {
    }

    static PokeApiPokemonJson pokemon(int id) {
        return read("pokemon-" + id + ".json", PokeApiPokemonJson.class);
    }

    static PokeApiSpeciesJson species(int id) {
        return read("pokemon-species-" + id + ".json", PokeApiSpeciesJson.class);
    }

    private static <T> T read(String file, Class<T> type) {
        try (InputStream json = PokeApiFixtures.class.getResourceAsStream("/pokeapi/" + file)) {
            if (json == null) {
                throw new IllegalArgumentException("No recorded PokeAPI response named " + file);
            }
            return JSON.readValue(json, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
