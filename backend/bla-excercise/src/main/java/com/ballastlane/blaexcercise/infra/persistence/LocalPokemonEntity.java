package com.ballastlane.blaexcercise.infra.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA mapping of the local_pokemon table. It only lives in infrastructure:
 * the domain uses the LocalPokemon record, and LocalPokemonMapper converts between them.
 */
@Entity
@Table(name = "local_pokemon")
public class LocalPokemonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pokeapi_id", nullable = false, unique = true)
    private int pokeapiId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "localized_name", length = 100)
    private String localizedName;

    @Column(length = 100)
    private String region;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "local_pokemon_tag", joinColumns = @JoinColumn(name = "local_pokemon_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA needs a no-arguments constructor to create the object when reading from the database.
     * It is protected (not public) so the rest of the code cannot create half-filled entities;
     * the mapper can use it because it is in the same package.
     */
    protected LocalPokemonEntity() {
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getPokeapiId() { return pokeapiId; }
    public void setPokeapiId(int pokeapiId) { this.pokeapiId = pokeapiId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocalizedName() { return localizedName; }
    public void setLocalizedName(String localizedName) { this.localizedName = localizedName; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
