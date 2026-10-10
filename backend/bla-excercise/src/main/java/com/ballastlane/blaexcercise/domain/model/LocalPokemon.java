package com.ballastlane.blaexcercise.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * US04: returns a copy with new values for our own fields only.
     * The official PokeAPI data (pokeapiId, name, image, description) is copied as it is,
     * so there is no way to change it through this method.
     */
    public LocalPokemon withCustomFields(String localizedName, String region, Set<String> tags) {
        return new LocalPokemon(
                id,
                pokeapiId,
                name,
                imageUrl,
                description,
                blankToNull(localizedName),
                blankToNull(region),
                normalizeTags(tags),
                createdAt,
                updatedAt);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * " Electric ", "electric" and "ELECTRIC" are the same tag. Blank tags are dropped.
     */
    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
