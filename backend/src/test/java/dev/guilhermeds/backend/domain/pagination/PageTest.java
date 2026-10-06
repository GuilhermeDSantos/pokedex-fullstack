package dev.guilhermeds.backend.domain.pagination;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {

    @Test
    void shouldKeepAnImmutableCopyOfTheContent() {
        var source = new ArrayList<>(List.of("bulbasaur", "ivysaur"));
        var page = new Page<>(source, 1302);

        source.add("venusaur");

        assertThat(page.content()).containsExactly("bulbasaur", "ivysaur");
        assertThatThrownBy(() -> page.content().add("charmander"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
