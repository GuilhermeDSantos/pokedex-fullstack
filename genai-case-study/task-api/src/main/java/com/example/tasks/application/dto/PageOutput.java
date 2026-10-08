package com.example.tasks.application.dto;

import java.util.List;

public record PageOutput<T>(
    List<T> content,
    int page,
    int size,
    long totalElements
) {

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }
}
