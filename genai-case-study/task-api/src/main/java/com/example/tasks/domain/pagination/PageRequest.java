package com.example.tasks.domain.pagination;

import com.example.tasks.domain.exception.InvalidPageRequestException;

public record PageRequest(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) {
            throw new InvalidPageRequestException("Page must be 0 or more");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidPageRequestException("Page size must be between 1 and " + MAX_SIZE);
        }
    }
}
