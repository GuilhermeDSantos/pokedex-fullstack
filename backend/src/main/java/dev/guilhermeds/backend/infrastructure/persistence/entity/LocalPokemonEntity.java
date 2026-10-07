package dev.guilhermeds.backend.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "local_pokemons")
public class LocalPokemonEntity {

    @Id
    private UUID id;

    @Column(name = "pokedex_number", nullable = false, unique = true)
    private int pokedexNumber;

    @Column(name = "localized_name")
    private String localizedName;

    private String region;

    // Tags are values of the record, not entities of their own (D-039).
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "local_pokemon_tags", joinColumns = @JoinColumn(name = "local_pokemon_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new HashSet<>();

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public LocalPokemonEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getPokedexNumber() { return pokedexNumber; }
    public void setPokedexNumber(int pokedexNumber) { this.pokedexNumber = pokedexNumber; }
    public String getLocalizedName() { return localizedName; }
    public void setLocalizedName(String localizedName) { this.localizedName = localizedName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
}
