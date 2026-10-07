package dev.guilhermeds.backend.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Our record of a Pokémon (US-03): its Pokédex number plus the fields that are ours. Everything
 * else, the name included, is read from the canonical data (D-039). New records come from
 * {@link #create}; {@link #builder()} only rebuilds one that was already valid when it was stored
 * (persistence mapper).
 */
public class LocalPokemon {

    private final LocalPokemonId id;
    private final PokedexNumber pokedexNumber;
    private CustomAttributes customAttributes;
    private final Instant syncedAt;
    private Instant updatedAt;

    private LocalPokemon(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id must not be null");
        this.pokedexNumber = Objects.requireNonNull(builder.pokedexNumber, "pokedexNumber must not be null");
        this.customAttributes = Objects.requireNonNull(builder.customAttributes, "customAttributes must not be null");
        this.syncedAt = Objects.requireNonNull(builder.syncedAt, "syncedAt must not be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "updatedAt must not be null");
    }

    public static LocalPokemon create(LocalPokemonId id, PokedexNumber pokedexNumber, Instant now) {
        return builder()
            .id(id)
            .pokedexNumber(pokedexNumber)
            .customAttributes(CustomAttributes.empty())
            .syncedAt(now)
            .updatedAt(now)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // PUT semantics (US-04): the given fields replace ours entirely.
    public void updateCustomAttributes(CustomAttributes attributes, Instant now) {
        this.customAttributes = Objects.requireNonNull(attributes, "attributes must not be null");
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    // The canonical name comes from the caller, which reads it from the canonical data.
    public String displayName(String canonicalName) {
        return customAttributes.localizedName() != null ? customAttributes.localizedName() : canonicalName;
    }

    public LocalPokemonId getId() { return id; }
    public PokedexNumber getPokedexNumber() { return pokedexNumber; }
    public CustomAttributes getCustomAttributes() { return customAttributes; }
    public Instant getSyncedAt() { return syncedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof LocalPokemon other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "LocalPokemon{id=" + id.value() + ", pokedexNumber=" + pokedexNumber.value() + "}";
    }

    public static final class Builder {

        private LocalPokemonId id;
        private PokedexNumber pokedexNumber;
        private CustomAttributes customAttributes;
        private Instant syncedAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder id(LocalPokemonId id) { this.id = id; return this; }
        public Builder pokedexNumber(PokedexNumber pokedexNumber) { this.pokedexNumber = pokedexNumber; return this; }
        public Builder customAttributes(CustomAttributes customAttributes) { this.customAttributes = customAttributes; return this; }
        public Builder syncedAt(Instant syncedAt) { this.syncedAt = syncedAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public LocalPokemon build() {
            return new LocalPokemon(this);
        }
    }
}
