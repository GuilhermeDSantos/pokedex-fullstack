package dev.guilhermeds.backend.domain.pagination;

import dev.guilhermeds.backend.domain.exception.InvalidPageRequestException;

public record PageRequest(int page, int size) {

    public PageRequest {
        if (page < 0) {
            throw new InvalidPageRequestException("page cannot be negative");
        }
    }
}
