package dev.guilhermeds.backend.interfaces.rest.request;

import dev.guilhermeds.backend.domain.model.CustomAttributes;
import jakarta.validation.constraints.Size;

import java.util.List;

// Sizes only, for a message per field; formats belong to Tag and CustomAttributes (D-028).
public record UpdateLocalPokemonRequest(
    @Size(max = CustomAttributes.MAX_TEXT_LENGTH) String localizedName,
    @Size(max = CustomAttributes.MAX_TEXT_LENGTH) String region,
    @Size(max = CustomAttributes.MAX_TAGS) List<String> tags
) {
}
