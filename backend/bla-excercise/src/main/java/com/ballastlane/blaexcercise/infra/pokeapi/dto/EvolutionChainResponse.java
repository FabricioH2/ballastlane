package com.ballastlane.blaexcercise.infra.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionChainResponse(int id, ChainLink chain) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChainLink(
            NamedApiResource species,
            @JsonProperty("evolves_to") List<ChainLink> evolvesTo
    ) {
    }
}
