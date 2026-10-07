package dev.guilhermeds.backend.interfaces.rest.request;

import java.util.List;

public record UpdateLocalPokemonRequest(String localizedName, String region, List<String> tags) {
}
