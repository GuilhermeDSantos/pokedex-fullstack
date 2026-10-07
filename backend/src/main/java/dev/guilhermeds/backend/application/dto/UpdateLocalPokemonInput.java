package dev.guilhermeds.backend.application.dto;

import java.util.List;

public record UpdateLocalPokemonInput(String pokedexNumber, String localizedName, String region, List<String> tags) {
}
