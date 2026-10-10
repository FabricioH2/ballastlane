package com.ballastlane.blaexcercise.infra.pokeapi;

import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonResponse;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the cache really works: a real (small) Spring context, so the @Cacheable proxy exists,
 * and WireMock counts how many requests actually reach the "PokeAPI".
 */
@SpringJUnitConfig(PokeApiClientCacheTest.CacheTestConfig.class)
class PokeApiClientCacheTest {

    private static final String BULBASAUR_JSON = """
            {
              "id": 1,
              "name": "bulbasaur",
              "weight": 69,
              "sprites": { "front_default": "https://img/1.png" },
              "abilities": [],
              "stats": [],
              "species": { "name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon-species/1/" }
            }
            """;

    @RegisterExtension
    static WireMockExtension pokeApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private PokeApiClient client;

    @Test
    void secondRequestForSamePokemonIsServedFromCache() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon/bulbasaur"))
                .willReturn(okJson(BULBASAUR_JSON)));

        PokemonResponse first = client.getPokemon("bulbasaur");
        PokemonResponse second = client.getPokemon("bulbasaur");

        assertThat(second).isSameAs(first);
        pokeApi.verify(1, getRequestedFor(urlPathEqualTo("/pokemon/bulbasaur")));
    }

    @Test
    void notFoundResponsesAreNotCached() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon/missingno"))
                .willReturn(notFound()));

        assertThatThrownBy(() -> client.getPokemon("missingno"))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThatThrownBy(() -> client.getPokemon("missingno"))
                .isInstanceOf(HttpClientErrorException.NotFound.class);

        pokeApi.verify(2, getRequestedFor(urlPathEqualTo("/pokemon/missingno")));
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager();
        }

        @Bean
        PokeApiClient pokeApiClient() {
            RestClient restClient = RestClient.builder()
                    .baseUrl(pokeApi.getRuntimeInfo().getHttpBaseUrl())
                    .build();
            return new PokeApiClient(restClient);
        }
    }
}
