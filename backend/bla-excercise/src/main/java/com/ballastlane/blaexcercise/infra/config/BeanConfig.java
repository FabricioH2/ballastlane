package com.ballastlane.blaexcercise.infra.config;

import com.ballastlane.blaexcercise.application.port.in.ListPokemonService;
import com.ballastlane.blaexcercise.application.port.in.LoginService;
import com.ballastlane.blaexcercise.application.port.in.PokemonDetailService;
import com.ballastlane.blaexcercise.application.port.in.RegisterUserService;
import com.ballastlane.blaexcercise.application.port.in.SyncPokemonService;
import com.ballastlane.blaexcercise.application.port.out.LocalPokemonRepository;
import com.ballastlane.blaexcercise.application.port.out.PasswordHasher;
import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.application.port.out.TokenIssuer;
import com.ballastlane.blaexcercise.application.port.out.UserRepository;
import com.ballastlane.blaexcercise.application.service.GetPokemonDetailServiceImpl;
import com.ballastlane.blaexcercise.application.service.ListPokemonServiceImpl;
import com.ballastlane.blaexcercise.application.service.LoginServiceImpl;
import com.ballastlane.blaexcercise.application.service.RegisterUserServiceImpl;
import com.ballastlane.blaexcercise.application.service.SyncPokemonServiceImpl;
import com.ballastlane.blaexcercise.infra.persistence.JpaLocalPokemonRepository;
import com.ballastlane.blaexcercise.infra.persistence.JpaUserRepository;
import com.ballastlane.blaexcercise.infra.persistence.SpringDataLocalPokemonRepository;
import com.ballastlane.blaexcercise.infra.persistence.SpringDataUserRepository;
import com.ballastlane.blaexcercise.infra.pokeapi.PokeApiCatalogAdapter;
import com.ballastlane.blaexcercise.infra.pokeapi.PokeApiClient;
import com.ballastlane.blaexcercise.infra.security.JwtProperties;
import com.ballastlane.blaexcercise.infra.security.JwtTokenIssuer;
import com.ballastlane.blaexcercise.infra.security.SpringPasswordHasher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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

    @Bean
    UserRepository userRepository(SpringDataUserRepository springDataRepository) {
        return new JpaUserRepository(springDataRepository);
    }

    // ---------- Security ----------

    @Bean
    PasswordHasher passwordHasher(PasswordEncoder passwordEncoder) {
        return new SpringPasswordHasher(passwordEncoder);
    }

    @Bean
    TokenIssuer tokenIssuer(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        return new JwtTokenIssuer(jwtEncoder, jwtProperties);
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

    @Bean
    RegisterUserService registerUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new RegisterUserServiceImpl(userRepository, passwordHasher);
    }

    @Bean
    LoginService loginService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        return new LoginServiceImpl(userRepository, passwordHasher, tokenIssuer);
    }
}
