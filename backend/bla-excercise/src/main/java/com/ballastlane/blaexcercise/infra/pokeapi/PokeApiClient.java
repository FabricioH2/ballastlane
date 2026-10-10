package com.ballastlane.blaexcercise.infra.pokeapi;

import com.ballastlane.blaexcercise.infra.pokeapi.dto.EvolutionChainResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonListResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonSpeciesResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP client for the PokeAPI: one method per endpoint, each response cached.
 * PokeAPI data almost never changes and its fair use policy asks clients to cache.
 * It is separate from the adapter so @Cacheable works: Spring only caches calls
 * that come from outside the class (through the proxy).
 */
public class PokeApiClient {

    public static final String POKEMON_PAGES_CACHE = "pokeapi-pokemon-pages";
    public static final String POKEMON_CACHE = "pokeapi-pokemon";
    public static final String SPECIES_CACHE = "pokeapi-species";
    public static final String EVOLUTION_CHAINS_CACHE = "pokeapi-evolution-chains";

    private final RestClient restClient;

    public PokeApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Cacheable(POKEMON_PAGES_CACHE)
    public PokemonListResponse getPokemonPage(int offset, int limit) {
        return restClient.get()
                .uri("/pokemon?offset={offset}&limit={limit}", offset, limit)
                .retrieve()
                .body(PokemonListResponse.class);
    }

    @Cacheable(POKEMON_CACHE)
    public PokemonResponse getPokemon(String idOrName) {
        return restClient.get()
                .uri("/pokemon/{idOrName}", idOrName)
                .retrieve()
                .body(PokemonResponse.class);
    }

    @Cacheable(SPECIES_CACHE)
    public PokemonSpeciesResponse getSpecies(int id) {
        return restClient.get()
                .uri("/pokemon-species/{id}", id)
                .retrieve()
                .body(PokemonSpeciesResponse.class);
    }

    @Cacheable(EVOLUTION_CHAINS_CACHE)
    public EvolutionChainResponse getEvolutionChain(int id) {
        return restClient.get()
                .uri("/evolution-chain/{id}", id)
                .retrieve()
                .body(EvolutionChainResponse.class);
    }

    public static RestClient createRestClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
