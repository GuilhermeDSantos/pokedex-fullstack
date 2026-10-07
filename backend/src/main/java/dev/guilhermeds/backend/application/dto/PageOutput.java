package dev.guilhermeds.backend.application.dto;

import dev.guilhermeds.backend.domain.pagination.Page;
import dev.guilhermeds.backend.domain.pagination.PageRequest;

import java.util.List;

// The application's page, so interfaces/ never sees the domain Page.
public record PageOutput<T>(
    List<T> content,
    int page,
    int size,
    long totalElements
) {

    public static <T> PageOutput<T> from(Page<T> page, PageRequest request) {
        return new PageOutput<>(page.content(), request.page(), request.size(), page.totalElements());
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
