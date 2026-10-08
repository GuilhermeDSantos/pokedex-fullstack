package com.example.tasks.domain.pagination;

public record PageRequest(int page, int size) {

    public static final int MAX_SIZE = 100;
}
