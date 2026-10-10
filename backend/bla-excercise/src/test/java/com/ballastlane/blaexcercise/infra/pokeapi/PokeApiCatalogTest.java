package com.ballastlane.blaexcercise.infra.pokeapi;

import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class PokeApiCatalogTest {

    @RegisterExtension
    static WireMockExtension pokeApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private PokeApiCatalog catalog;

    @BeforeEach
    void setUp() {
        RestClient restClient = RestClient.builder()
                .baseUrl(pokeApi.getRuntimeInfo().getHttpBaseUrl())
                .build();
        catalog = new PokeApiCatalog(restClient);
    }

    @Test
    void mapsPokeApiResponsesToPokemonSummaries() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon"))
                .withQueryParam("offset", equalTo("0"))
                .withQueryParam("limit", equalTo("1"))
                .willReturn(okJson(fixture("pokemon-list.json"))));
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon/bulbasaur"))
                .willReturn(okJson(fixture("pokemon-bulbasaur.json"))));
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon-species/1"))
                .willReturn(okJson(fixture("pokemon-species-1.json"))));

        PageResult<PokemonSummary> result = catalog.findAll(0, 1);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(1302);
        assertThat(result.items()).containsExactly(new PokemonSummary(
                1,
                "bulbasaur",
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                "Seed Pokémon",   // English genus, even though it is not the first entry
                6.9,              // 69 hectograms -> 6.9 kg
                List.of("overgrow", "chlorophyll")));
    }

    @Test
    void translatesPageAndSizeIntoOffsetAndLimit() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon"))
                .willReturn(okJson("""
                        { "count": 1302, "results": [] }
                        """)));

        PageResult<PokemonSummary> result = catalog.findAll(2, 20);

        pokeApi.verify(getRequestedFor(urlPathEqualTo("/pokemon"))
                .withQueryParam("offset", equalTo("40"))
                .withQueryParam("limit", equalTo("20")));
        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1302);
    }

    private static String fixture(String name) {
        try (InputStream in = PokeApiCatalogTest.class.getResourceAsStream("/pokeapi/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Missing test fixture: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
