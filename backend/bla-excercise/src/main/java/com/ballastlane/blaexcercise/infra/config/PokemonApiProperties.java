package com.ballastlane.blaexcercise.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pokeapi")
public record PokemonApiProperties(String baseUrl) {
}
