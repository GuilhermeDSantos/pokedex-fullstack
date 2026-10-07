package dev.guilhermeds.backend.application.dto;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageOutputTest {

    @ParameterizedTest
    @CsvSource({
        "1351, 20, 68",
        "1340, 20, 67",
        "0,    20, 0"
    })
    void shouldCountTheLastPartialPageAsAPage(long totalElements, int size, int totalPages) {
        assertThat(new PageOutput<>(List.of(), 0, size, totalElements).totalPages()).isEqualTo(totalPages);
    }
}
