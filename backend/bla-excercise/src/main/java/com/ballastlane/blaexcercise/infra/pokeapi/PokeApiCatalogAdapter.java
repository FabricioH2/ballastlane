package com.ballastlane.blaexcercise.infra.pokeapi;

import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemoDetail;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;
import com.ballastlane.blaexcercise.domain.model.Stat;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.EvolutionChainResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonListResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonSpeciesResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Output adapter: implements PokemonCatalog using the public PokeAPI.
 * It is the only class that knows the PokeAPI format; it translates it into domain models.
 */
public class PokeApiCatalogAdapter implements PokemonCatalog {

    private static final String ENGLISH = "en";
    private static final double HECTOGRAMS_PER_KILOGRAM = 10.0;

    private final RestClient restClient;

    public PokeApiCatalogAdapter(RestClient restClient) {
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


    @Override
    public Optional<PokemoDetail> findByIdOrName(String idOrName) {
        return fetchPokemonIfExists(idOrName).map(this::toDetail);
    }

    private Optional<PokemonResponse> fetchPokemonIfExists(String idOrName) {
        try {
            return Optional.ofNullable(fetchPokemon(idOrName));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    private PokemoDetail toDetail(PokemonResponse pokemon) {
        PokemonSpeciesResponse species = fetchSpecies(extractId(pokemon.species().url()));
        EvolutionChainResponse evolution = fetchEvolutionChain(extractId(species.evolutionChain().url()));

        return new PokemoDetail(
                pokemon.id(),
                pokemon.name(),
                officialArtwork(pokemon),
                stats(pokemon),
                englishDescription(species),
                evolutionNames(evolution.chain()));
    }


    private PokemonResponse fetchPokemon(String idOrName) {
        return restClient.get()
                .uri("/pokemon/{idOrName}", idOrName)
                .retrieve()
                .body(PokemonResponse.class);
    }

    private PokemonSpeciesResponse fetchSpecies(int id) {
        return restClient.get()
                .uri("/pokemon-species/{id}", id)
                .retrieve()
                .body(PokemonSpeciesResponse.class);
    }

    private EvolutionChainResponse fetchEvolutionChain(int id) {
        return restClient.get()
                .uri("/evolution-chain/{id}", id)
                .retrieve()
                .body(EvolutionChainResponse.class);
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

    private static String officialArtwork(PokemonResponse pokemon) {
        PokemonResponse.Sprites sprites = pokemon.sprites();
        return Optional.ofNullable(sprites.other())
                .map(PokemonResponse.OtherSprites::officialArtwork)
                .map(PokemonResponse.Artwork::frontDefault)
                .orElse(sprites.frontDefault());
    }

    private static List<Stat> stats(PokemonResponse pokemon) {
        return pokemon.stats().stream()
                .map(entry -> new Stat(entry.stat().name(), entry.baseStat()))
                .toList();
    }

    private static String englishDescription(PokemonSpeciesResponse species) {
        return species.flavorTextEntries().stream()
                .filter(entry -> ENGLISH.equals(entry.language().name()))
                .map(PokemonSpeciesResponse.FlavorTextEntry::flavorText)
                .map(PokeApiCatalogAdapter::cleanText)
                .findFirst()
                .orElse(null);
    }

    /**
     * PokeAPI descriptions come from the games and contain line breaks (\n) and page breaks (\f).
     */
    private static String cleanText(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Flattens the recursive chain: bulbasaur -> [ivysaur -> [venusaur]] becomes
     * [bulbasaur, ivysaur, venusaur]. Branches (like Eevee) are visited in order.
     */
    static List<String> evolutionNames(EvolutionChainResponse.ChainLink chain) {
        List<String> names = new ArrayList<>();
        collectNames(chain, names);
        return names;
    }

    private static void collectNames(EvolutionChainResponse.ChainLink link, List<String> names) {
        names.add(link.species().name());
        if (link.evolvesTo() == null) {
            return;
        }
        for (EvolutionChainResponse.ChainLink next : link.evolvesTo()) {
            collectNames(next, names);
        }
    }

    /**
     * PokeAPI URLs end with the resource id: ".../pokemon-species/1/" -> 1.
     */
    static int extractId(String url) {
        String withoutTrailingSlash = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return Integer.parseInt(withoutTrailingSlash.substring(withoutTrailingSlash.lastIndexOf('/') + 1));
    }
}
