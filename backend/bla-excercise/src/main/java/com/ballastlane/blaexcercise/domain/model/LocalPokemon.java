package com.ballastlane.blaexcercise.domain.model;

import java.time.Instant;
import java.util.Set;

/**
 * A Pokemon saved in our own database: a copy of the official PokeAPI data
 * plus our own fields (localized name, region, tags).
 * id, createdAt and updatedAt are null until it is saved.
 */
public record LocalPokemon(
        Long id,
        int pokeapiId,
        String name,
        String imageUrl,
        String description,
        String localizedName,
        String region,
        Set<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
    public LocalPokemon {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    /**
     * Creates a new, not yet saved, local Pokemon from the official data.
     * Our own fields start empty and are edited later (US04).
     */
    public static LocalPokemon newFrom(PokemonDetail detail) {
        return new LocalPokemon(
                null,
                detail.id(),
                detail.name(),
                detail.imageUrl(),
                detail.description(),
                null,
                null,
                Set.of(),
                null,
                null);
    }
}
