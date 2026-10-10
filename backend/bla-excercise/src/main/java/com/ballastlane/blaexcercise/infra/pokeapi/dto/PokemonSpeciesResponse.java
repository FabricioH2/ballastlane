package com.ballastlane.blaexcercise.infra.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GET /pokemon-species/{id}. "genera" holds the category in many languages.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonSpeciesResponse(int id, String name, List<Genus> genera,
                                     @JsonProperty("flavor_text_entries") List<FlavorTextEntry> flavorTextEntries,
                                     @JsonProperty("evolution_chain") ApiResource evolutionChain) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genus(String genus, NamedApiResource language) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlavorTextEntry(@JsonProperty("flavor_text") String flavorText, NamedApiResource language) {
    }
}
