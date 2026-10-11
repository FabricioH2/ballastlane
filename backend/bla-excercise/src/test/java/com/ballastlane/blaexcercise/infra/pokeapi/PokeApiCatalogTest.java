package com.ballastlane.blaexcercise.infra.pokeapi;

import com.ballastlane.blaexcercise.domain.exception.PokemonCatalogUnavailableException;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonDetail;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;
import com.ballastlane.blaexcercise.domain.model.Stat;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.EvolutionChainResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.NamedApiResource;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class PokeApiCatalogTest {

    @RegisterExtension
    static WireMockExtension pokeApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);

    private PokeApiCatalogAdapter catalog;

    @BeforeEach
    void setUp() {
        // Generous read timeout: on a busy machine (or with the JaCoCo agent) the first request can take
        // longer than 500 ms, which made the tests flaky. The "too slow" test uses a much longer delay.
        RestClient restClient = PokeApiClient.createRestClient(
                pokeApi.getRuntimeInfo().getHttpBaseUrl(),
                Duration.ofSeconds(2),
                READ_TIMEOUT);
        catalog = new PokeApiCatalogAdapter(new PokeApiClient(restClient));
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

    @Test
    void mapsPokeApiResponsesToPokemonDetail() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon/bulbasaur"))
                .willReturn(okJson(fixture("pokemon-bulbasaur.json"))));
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon-species/1"))
                .willReturn(okJson(fixture("pokemon-species-1.json"))));
        pokeApi.stubFor(get(urlPathEqualTo("/evolution-chain/1"))
                .willReturn(okJson(fixture("evolution-chain-1.json"))));

        Optional<PokemonDetail> result = catalog.findByIdOrName("bulbasaur");

        assertThat(result).contains(new PokemonDetail(
                1,
                "bulbasaur",
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png",
                List.of(new Stat("hp", 45), new Stat("attack", 49)),
                "A strange seed was planted on its back at birth.",
                List.of("bulbasaur", "ivysaur", "venusaur")));
    }

    @Test
    void returnsEmptyWhenPokemonDoesNotExist() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon/missingno"))
                .willReturn(notFound()));

        assertThat(catalog.findByIdOrName("missingno")).isEmpty();
    }

    @Test
    void flattensBranchingEvolutionChains() {
        var eevee = link("eevee", link("vaporeon"), link("jolteon"), link("flareon"));

        assertThat(PokeApiCatalogAdapter.evolutionNames(eevee))
                .containsExactly("eevee", "vaporeon", "jolteon", "flareon");
    }

    private static EvolutionChainResponse.ChainLink link(String name, EvolutionChainResponse.ChainLink... next) {
        return new EvolutionChainResponse.ChainLink(
                new NamedApiResource(name, "https://pokeapi.co/api/v2/pokemon-species/"),
                List.of(next));
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

    @Test
    void throwsCatalogUnavailableWhenPokeApiFails() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon"))
                .willReturn(serverError()));

        assertThatThrownBy(() -> catalog.findAll(0, 20))
                .isInstanceOf(PokemonCatalogUnavailableException.class);
    }

    @Test
    void throwsCatalogUnavailableWhenPokeApiIsTooSlow() {
        pokeApi.stubFor(get(urlPathEqualTo("/pokemon/bulbasaur"))
                .willReturn(okJson(fixture("pokemon-bulbasaur.json"))
                        .withFixedDelay((int) READ_TIMEOUT.multipliedBy(2).toMillis())));

        assertThatThrownBy(() -> catalog.findByIdOrName("bulbasaur"))
                .isInstanceOf(PokemonCatalogUnavailableException.class);
    }
}
