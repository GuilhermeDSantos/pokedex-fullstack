package dev.guilhermeds.backend.domain.pagination;

import dev.guilhermeds.backend.domain.exception.InvalidPageRequestException;

public record PageRequest(int page, int size) {

    // Each item of a PokeAPI list page costs two upstream calls.
    public static final int MAX_SIZE = 50;

    public PageRequest {
        if (page < 0) {
            throw new InvalidPageRequestException("page cannot be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidPageRequestException("size must be between 1 and " + MAX_SIZE);
        }
    }

    public long offset() {
        return (long) page * size;
    }
}
