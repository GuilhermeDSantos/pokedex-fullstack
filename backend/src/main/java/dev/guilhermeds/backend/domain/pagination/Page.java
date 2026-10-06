package dev.guilhermeds.backend.domain.pagination;

import java.util.List;

public record Page<T>(List<T> content, long totalElements) {

    public Page {
        content = List.copyOf(content);
    }
}
