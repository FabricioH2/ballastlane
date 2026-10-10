package com.ballastlane.blaexcercise.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PaginationProperties.class, PokemonApiProperties.class})
public class BeanConfig {
}
