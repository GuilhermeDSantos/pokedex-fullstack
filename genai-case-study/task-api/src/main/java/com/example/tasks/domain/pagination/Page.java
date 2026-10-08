package com.example.tasks.domain.pagination;

import java.util.List;

public record Page<T>(List<T> content, long totalElements) {

    public Page {
        content = List.copyOf(content);
    }
}
