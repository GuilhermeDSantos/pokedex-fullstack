package dev.guilhermeds.backend.domain.model;

import dev.guilhermeds.backend.domain.exception.InvalidCustomAttributesException;
import dev.guilhermeds.backend.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomAttributesTest {

    // A Pokémon is synced with none of our own fields yet: they are filled in later (US-04).
    @Test
    void shouldStartEmpty() {
        var empty = CustomAttributes.empty();

        assertThat(empty.localizedName()).isNull();
        assertThat(empty.region()).isNull();
        assertThat(empty.tags()).isEmpty();
    }

    // An emptied field in the form means "not set", not a blank value to display.
    @Test
    void shouldTrimTheTextsAndTreatBlankAsNotSet() {
        var attributes = new CustomAttributes("  Pica ", "   ", Set.of());

        assertThat(attributes.localizedName()).isEqualTo("Pica");
        assertThat(attributes.region()).isNull();
    }

    @Test
    void shouldAcceptTextsUpToTheMaximumLength() {
        var longest = "a".repeat(CustomAttributes.MAX_TEXT_LENGTH);

        assertThat(new CustomAttributes(longest, longest, Set.of()).region()).hasSize(100);
    }

    @Test
    void shouldRejectALocalizedNameOrRegionOverTheMaximumLengthAsAValidationError() {
        var tooLong = "a".repeat(101);

        assertThatThrownBy(() -> new CustomAttributes(tooLong, null, Set.of()))
            .isInstanceOf(InvalidCustomAttributesException.class)
            .isInstanceOf(ValidationException.class)
            .hasMessage("Localized name must be at most 100 characters");
        assertThatThrownBy(() -> new CustomAttributes(null, tooLong, Set.of()))
            .isInstanceOf(InvalidCustomAttributesException.class)
            .hasMessage("Region must be at most 100 characters");
    }

    @Test
    void shouldAcceptUpToTheMaximumNumberOfTags() {
        assertThat(new CustomAttributes(null, null, tags(CustomAttributes.MAX_TAGS)).tags()).hasSize(10);
    }

    @Test
    void shouldRejectMoreThanTheMaximumNumberOfTagsAsAValidationError() {
        assertThatThrownBy(() -> new CustomAttributes(null, null, tags(11)))
            .isInstanceOf(InvalidCustomAttributesException.class)
            .hasMessage("A Pokémon has at most 10 tags");
    }

    @Test
    void shouldKeepItsOwnUnmodifiableCopyOfTheTags() {
        var given = new HashSet<>(Set.of(new Tag("starter")));
        var attributes = new CustomAttributes(null, null, given);

        given.add(new Tag("mascot"));

        assertThat(attributes.tags()).containsExactly(new Tag("starter"));
        assertThatThrownBy(() -> attributes.tags().add(new Tag("mascot")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Set<Tag> tags(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> new Tag("tag-" + i)).collect(Collectors.toSet());
    }
}
