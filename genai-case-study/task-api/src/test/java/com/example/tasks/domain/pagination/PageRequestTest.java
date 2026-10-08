package com.example.tasks.domain.pagination;

import com.example.tasks.domain.exception.InvalidPageRequestException;
import com.example.tasks.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @Test
    void shouldAcceptTheBoundaries() {
        assertThat(new PageRequest(0, 1).size()).isEqualTo(1);
        assertThat(new PageRequest(0, PageRequest.MAX_SIZE).size()).isEqualTo(100);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void shouldRejectAPageSizeOutsideOneToAHundred(int size) {
        assertThatThrownBy(() -> new PageRequest(0, size))
            .isInstanceOf(InvalidPageRequestException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("Page size must be between 1 and 100");
    }

    @Test
    void shouldRejectANegativePage() {
        assertThatThrownBy(() -> new PageRequest(-1, 20))
            .isInstanceOf(InvalidPageRequestException.class)
            .hasMessage("Page must be 0 or more");
    }
}
