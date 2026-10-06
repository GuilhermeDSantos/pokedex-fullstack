package dev.guilhermeds.backend.domain.pagination;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> content, long totalElements) {

    public Page {
        content = List.copyOf(content);
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(content.stream().map(mapper).toList(), totalElements);
    }
}
