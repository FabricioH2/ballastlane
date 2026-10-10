package com.ballastlane.blaexcercise.infra.pokeapi;

import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonListResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonSpeciesResponse;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

/**
 * Output adapter: implements PokemonCatalog using the public PokeAPI.
 * It is the only class that knows the PokeAPI format; it translates it into domain models.
 */
public class PokeApiCatalog implements PokemonCatalog {

    private static final String ENGLISH = "en";
    private static final double HECTOGRAMS_PER_KILOGRAM = 10.0;

    private final RestClient restClient;

    public PokeApiCatalog(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PageResult<PokemonSummary> findAll(int page, int size) {
        int offset = page * size;

        PokemonListResponse list = restClient.get()
                .uri("/pokemon?offset={offset}&limit={limit}", offset, size)
                .retrieve()
                .body(PokemonListResponse.class);

        List<PokemonSummary> items = list.results().stream()
                .map(resource -> toSummary(resource.name()))
                .toList();

        return new PageResult<>(items, page, size, list.count());
    }

    private PokemonSummary toSummary(String name) {
        PokemonResponse pokemon = fetchPokemon(name);
        PokemonSpeciesResponse species = fetchSpecies(extractId(pokemon.species().url()));

        return new PokemonSummary(
                pokemon.id(),
                pokemon.name(),
                pokemon.sprites().frontDefault(),
                englishCategory(species),
                pokemon.weight() / HECTOGRAMS_PER_KILOGRAM,
                abilityNames(pokemon));
    }

    private PokemonResponse fetchPokemon(String name) {
        return restClient.get()
                .uri("/pokemon/{name}", name)
                .retrieve()
                .body(PokemonResponse.class);
    }

    private PokemonSpeciesResponse fetchSpecies(int id) {
        return restClient.get()
                .uri("/pokemon-species/{id}", id)
                .retrieve()
                .body(PokemonSpeciesResponse.class);
    }

    private static String englishCategory(PokemonSpeciesResponse species) {
        return species.genera().stream()
                .filter(genus -> ENGLISH.equals(genus.language().name()))
                .map(PokemonSpeciesResponse.Genus::genus)
                .findFirst()
                .orElse(null);
    }

    private static List<String> abilityNames(PokemonResponse pokemon) {
        return pokemon.abilities().stream()
                .sorted(Comparator.comparingInt(PokemonResponse.AbilitySlot::slot))
                .map(abilitySlot -> abilitySlot.ability().name())
                .toList();
    }

    /**
     * PokeAPI URLs end with the resource id: ".../pokemon-species/1/" -> 1.
     */
    static int extractId(String url) {
        String withoutTrailingSlash = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return Integer.parseInt(withoutTrailingSlash.substring(withoutTrailingSlash.lastIndexOf('/') + 1));
    }
}
