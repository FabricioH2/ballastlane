package com.ballastlane.blaexcercise.infra.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GET /pokemon/{idOrName}. Only the fields we use; the rest are ignored.
 * weight comes in hectograms (69 = 6.9 kg).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonResponse(
        int id,
        String name,
        int weight,
        Sprites sprites,
        List<AbilitySlot> abilities,
        NamedApiResource species,
        List<StatEntry> stats
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprites(@JsonProperty("front_default") String frontDefault, OtherSprites other) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilitySlot(NamedApiResource ability, int slot) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OtherSprites(@JsonProperty("official-artwork") Artwork officialArtwork) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artwork(@JsonProperty("front_default") String frontDefault) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatEntry(@JsonProperty("base_stat") int baseStat, NamedApiResource stat) {
    }
}
