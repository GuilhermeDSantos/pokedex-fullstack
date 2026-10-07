package dev.guilhermeds.backend.domain.pagination;

import dev.guilhermeds.backend.domain.exception.InvalidPageRequestException;

public record PageRequest(int page, int size) {

    public static final int MIN_SIZE = 1;
    // Each item of a page costs two reads of the Pokémon repository.
    public static final int MAX_SIZE = 50;

    public PageRequest {
        if (page < 0) {
            throw InvalidPageRequestException.negativePage();
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw InvalidPageRequestException.sizeOutOfRange(MIN_SIZE, MAX_SIZE);
        }
    }

    public long offset() {
        return (long) page * size;
    }
}
