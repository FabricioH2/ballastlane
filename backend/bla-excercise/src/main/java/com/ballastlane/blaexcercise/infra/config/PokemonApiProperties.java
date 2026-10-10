package com.ballastlane.blaexcercise.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.pokeapi")
public record PokemonApiProperties(
        String baseUrl,
        @DefaultValue("3s") Duration connectTimeOut,
        @DefaultValue("5s") Duration readTimeout
        ) {
}
