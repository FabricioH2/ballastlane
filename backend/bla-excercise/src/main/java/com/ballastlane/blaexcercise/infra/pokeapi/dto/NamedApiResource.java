package com.ballastlane.blaexcercise.infra.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * PokeAPI's generic reference to another resource: {"name": "...", "url": "..."}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NamedApiResource(String name, String url) {
}
