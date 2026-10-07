package dev.guilhermeds.backend.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseStatTest {

    // Base stats run from 1 to 255 in the games; anything else means the mapping broke.
    @ParameterizedTest
    @ValueSource(ints = {0, 256})
    void shouldRejectAValueOutsideTheGamesRange(int value) {
        assertThatThrownBy(() -> new BaseStat(StatName.HP, value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("a base stat runs from 1 to 255");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 255})
    void shouldAcceptTheEndsOfTheRange(int value) {
        assertThat(new BaseStat(StatName.HP, value).value()).isEqualTo(value);
    }
}
