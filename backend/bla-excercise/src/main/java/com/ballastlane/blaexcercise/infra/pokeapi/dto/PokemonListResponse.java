package com.ballastlane.blaexcercise.infra.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * GET /pokemon?offset=&limit=
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonListResponse(int count, List<NamedApiResource> results) {
}
