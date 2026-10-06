package dev.guilhermeds.backend.domain.pagination;

import dev.guilhermeds.backend.domain.exception.InvalidPageRequestException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @Test
    void shouldRejectNegativePageAsValidationError() {
        assertThatThrownBy(() -> new PageRequest(-1, 20))
            .isInstanceOf(InvalidPageRequestException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("page cannot be negative");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 51})
    void shouldRejectSizeOutsideOneToFifty(int size) {
        assertThatThrownBy(() -> new PageRequest(0, size))
            .isInstanceOf(InvalidPageRequestException.class)
            .hasMessage("size must be between 1 and 50");
    }
}
