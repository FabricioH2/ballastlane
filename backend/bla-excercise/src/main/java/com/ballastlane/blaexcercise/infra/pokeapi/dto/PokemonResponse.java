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
        NamedApiResource species
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprites(@JsonProperty("front_default") String frontDefault) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilitySlot(NamedApiResource ability, int slot) {
    }
}
