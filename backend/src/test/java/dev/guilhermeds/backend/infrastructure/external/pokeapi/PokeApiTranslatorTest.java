package dev.guilhermeds.backend.infrastructure.external.pokeapi;

import dev.guilhermeds.backend.domain.model.Ability;
import dev.guilhermeds.backend.domain.model.BaseStat;
import dev.guilhermeds.backend.domain.model.Height;
import dev.guilhermeds.backend.domain.model.PokedexNumber;
import dev.guilhermeds.backend.domain.model.PokemonType;
import dev.guilhermeds.backend.domain.model.StatName;
import dev.guilhermeds.backend.domain.model.Weight;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.evolutionChain;
import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.pokemon;
import static dev.guilhermeds.backend.infrastructure.external.pokeapi.PokeApiFixtures.species;
import static org.assertj.core.api.Assertions.assertThat;

class PokeApiTranslatorTest {

    private final PokeApiTranslator translator = new PokeApiTranslator();

    @Test
    void shouldTakeTheNumberAndNameFromThePokemon() {
        var summary = translator.toSummary(pokemon(25), species(25));

        assertThat(summary.number()).isEqualTo(new PokedexNumber(25));
        assertThat(summary.name()).isEqualTo("pikachu");
    }

    // PokeAPI sends hectograms: Pikachu's 60 is 6.0 kg, the brief's "mass in kg".
    @Test
    void shouldConvertTheWeightToKilograms() {
        assertThat(translator.toSummary(pokemon(25), species(25)).weight()).isEqualTo(Weight.fromHectograms(60));
        assertThat(translator.toSummary(pokemon(25), species(25)).weight().kilograms()).isEqualByComparingTo("6.0");
    }

    // The category is the species' English genus (D-010); genera holds one entry per language.
    @Test
    void shouldUseTheEnglishGenusAsTheCategory() {
        assertThat(translator.toSummary(pokemon(25), species(25)).category()).isEqualTo("Mouse Pokémon");
    }

    @Test
    void shouldLeaveTheCategoryEmptyWhenThereIsNoEnglishGenus() {
        var pikachu = species(25);
        var onlyFrench = new PokeApiSpeciesJson(
            List.of(new PokeApiSpeciesJson.Genus("Pokémon Souris", new NamedResource("fr", "https://pokeapi.co/api/v2/language/5/"))),
            pikachu.flavorTextEntries(), pikachu.evolutionChain());

        assertThat(translator.toSummary(pokemon(25), onlyFrench).category()).isNull();
    }

    @Test
    void shouldUseTheDefaultFrontSpriteAsTheListImage() {
        assertThat(translator.toSummary(pokemon(25), species(25)).spriteUrl())
            .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png");
    }

    // Some alternate forms have no sprite at all.
    @Test
    void shouldAcceptAPokemonWithoutASprite() {
        var pikachu = pokemon(25);
        var withoutSprite = new PokeApiPokemonJson(pikachu.id(), pikachu.name(), pikachu.height(), pikachu.weight(),
            new PokeApiPokemonJson.Sprites(null, null), pikachu.types(), pikachu.abilities(), pikachu.stats(),
            pikachu.species());

        assertThat(translator.toSummary(withoutSprite, species(25)).spriteUrl()).isNull();
    }

    // The slot is the order players know (Bulbasaur is grass, then poison); the array order isn't guaranteed.
    @Test
    void shouldListTheTypesInSlotOrder() {
        var bulbasaur = pokemon(1);
        var shuffled = new PokeApiPokemonJson(bulbasaur.id(), bulbasaur.name(), bulbasaur.height(), bulbasaur.weight(),
            bulbasaur.sprites(), bulbasaur.types().reversed(), bulbasaur.abilities(), bulbasaur.stats(), bulbasaur.species());

        assertThat(translator.toSummary(shuffled, species(1)).types())
            .containsExactly(new PokemonType("grass"), new PokemonType("poison"));
    }

    // The brief's skills (D-010), in slot order, keeping which one is hidden.
    @Test
    void shouldListTheAbilitiesInSlotOrderWithTheHiddenOneMarked() {
        var pikachu = pokemon(25);
        var shuffled = new PokeApiPokemonJson(pikachu.id(), pikachu.name(), pikachu.height(), pikachu.weight(),
            pikachu.sprites(), pikachu.types(), pikachu.abilities().reversed(), pikachu.stats(), pikachu.species());

        assertThat(translator.toSummary(shuffled, species(25)).abilities())
            .containsExactly(new Ability("static", false), new Ability("lightning-rod", true));
    }

    // ---- detail (US-02) ---------------------------------------------------------------------

    @Test
    void shouldTranslateTheProfileWithHeightArtworkAndStatsInTheGamesOrder() {
        var detail = translator.toDetail(pokemon(25), species(25), evolutionChain(10));

        assertThat(detail.number()).isEqualTo(new PokedexNumber(25));
        assertThat(detail.profile().height()).isEqualTo(Height.fromDecimetres(4));
        assertThat(detail.profile().artworkUrl())
            .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png");
        assertThat(detail.profile().stats()).containsExactly(
            new BaseStat(StatName.HP, 35), new BaseStat(StatName.ATTACK, 55), new BaseStat(StatName.DEFENSE, 40),
            new BaseStat(StatName.SPECIAL_ATTACK, 50), new BaseStat(StatName.SPECIAL_DEFENSE, 50),
            new BaseStat(StatName.SPEED, 90));
    }

    // Legends: Arceus (version 39) is Pikachu's newest English entry; the array order isn't guaranteed.
    @Test
    void shouldDescribeThePokemonWithTheNewestEnglishEntry() {
        var pikachu = species(25);
        var shuffled = new PokeApiSpeciesJson(pikachu.genera(), pikachu.flavorTextEntries().reversed(), pikachu.evolutionChain());

        assertThat(translator.toDetail(pokemon(25), shuffled, evolutionChain(10)).profile().description())
            .startsWith("Possesses cheek sacs in which it stores electricity.");
    }

    // PokeAPI keeps the games' line breaks and form feeds (read as single spaces) and soft hyphens (dropped).
    @Test
    void shouldNormalizeTheBreaksInTheDescription() {
        var pikachu = species(25);
        var entry = new PokeApiSpeciesJson.FlavorText("When several\nof these POKé\u00adMON\fgather,  their electricity\ncould build.",
            new NamedResource("en", "https://pokeapi.co/api/v2/language/9/"),
            new NamedResource("red", "https://pokeapi.co/api/v2/version/1/"));
        var oneEntry = new PokeApiSpeciesJson(pikachu.genera(), List.of(entry), pikachu.evolutionChain());

        assertThat(translator.toDetail(pokemon(25), oneEntry, evolutionChain(10)).profile().description())
            .isEqualTo("When several of these POKéMON gather, their electricity could build.");
    }
}
