package com.ballastlane.blaexcercise.infra.config;

import com.ballastlane.blaexcercise.application.port.in.ListPokemonService;
import com.ballastlane.blaexcercise.application.port.in.PokemonDetailService;
import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.application.service.GetPokemonDetailServiceImpl;
import com.ballastlane.blaexcercise.application.service.ListPokemonServiceImpl;
import com.ballastlane.blaexcercise.infra.pokeapi.PokeApiCatalogAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({PaginationProperties.class, PokemonApiProperties.class})
public class BeanConfig {

    @Bean
    RestClient pokeApiRestClient(PokemonApiProperties pokeApiProperties) {
        return RestClient.builder()
                .baseUrl(pokeApiProperties.baseUrl())
                .build();
    }

    @Bean
    PokemonCatalog pokemonCatalog(RestClient pokeApiRestClient) {
        return new PokeApiCatalogAdapter(pokeApiRestClient);
    }

    @Bean
    ListPokemonService listPokemonService(PokemonCatalog pokemonCatalog,
                                          PaginationProperties paginationProperties) {
        return new ListPokemonServiceImpl(pokemonCatalog, paginationProperties.maxPageSize());
    }

    @Bean
    PokemonDetailService pokemonDetailService(PokemonCatalog pokemonCatalog) {
        return new GetPokemonDetailServiceImpl(pokemonCatalog);
    }
}
