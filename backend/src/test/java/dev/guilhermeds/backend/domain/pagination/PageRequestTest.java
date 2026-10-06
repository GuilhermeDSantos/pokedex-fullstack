package dev.guilhermeds.backend.domain.pagination;

import dev.guilhermeds.backend.domain.exception.InvalidPageRequestException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @Test
    void shouldRejectNegativePageAsValidationError() {
        assertThatThrownBy(() -> new PageRequest(-1, 20))
            .isInstanceOf(InvalidPageRequestException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("page cannot be negative");
    }
}
