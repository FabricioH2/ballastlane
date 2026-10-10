package com.ballastlane.blaexcercise.infra.config;

import com.ballastlane.blaexcercise.application.port.in.ListPokemonService;
import com.ballastlane.blaexcercise.application.port.in.PokemonDetailService;
import com.ballastlane.blaexcercise.application.port.in.SyncPokemonService;
import com.ballastlane.blaexcercise.application.port.out.LocalPokemonRepository;
import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.application.service.GetPokemonDetailServiceImpl;
import com.ballastlane.blaexcercise.application.service.ListPokemonServiceImpl;
import com.ballastlane.blaexcercise.application.service.SyncPokemonServiceImpl;
import com.ballastlane.blaexcercise.infra.persistence.JpaLocalPokemonRepository;
import com.ballastlane.blaexcercise.infra.persistence.SpringDataLocalPokemonRepository;
import com.ballastlane.blaexcercise.infra.pokeapi.PokeApiCatalogAdapter;
import com.ballastlane.blaexcercise.infra.pokeapi.PokeApiClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({PaginationProperties.class, PokemonApiProperties.class})
public class BeanConfig {

    // ---------- PokeAPI ----------

    @Bean
    RestClient pokeApiRestClient(PokemonApiProperties pokeApiProperties) {
        return PokeApiClient.createRestClient(
                pokeApiProperties.baseUrl(),
                pokeApiProperties.connectTimeout(),
                pokeApiProperties.readTimeout());
    }

    @Bean
    PokeApiClient pokeApiClient(RestClient pokeApiRestClient) {
        return new PokeApiClient(pokeApiRestClient);
    }

    @Bean
    PokemonCatalog pokemonCatalog(PokeApiClient pokeApiClient) {
        return new PokeApiCatalogAdapter(pokeApiClient);
    }

    // ---------- Database ----------

    @Bean
    LocalPokemonRepository localPokemonRepository(SpringDataLocalPokemonRepository springDataRepository) {
        return new JpaLocalPokemonRepository(springDataRepository);
    }

    // ---------- Use cases ----------

    @Bean
    ListPokemonService listPokemonService(PokemonCatalog pokemonCatalog,
                                          PaginationProperties paginationProperties) {
        return new ListPokemonServiceImpl(pokemonCatalog, paginationProperties.maxPageSize());
    }

    @Bean
    PokemonDetailService pokemonDetailService(PokemonCatalog pokemonCatalog) {
        return new GetPokemonDetailServiceImpl(pokemonCatalog);
    }

    @Bean
    SyncPokemonService syncPokemonService(PokemonDetailService pokemonDetailService,
                                          LocalPokemonRepository localPokemonRepository) {
        return new SyncPokemonServiceImpl(pokemonDetailService, localPokemonRepository);
    }
}
