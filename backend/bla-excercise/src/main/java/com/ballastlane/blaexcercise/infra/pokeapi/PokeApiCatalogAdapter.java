package com.ballastlane.blaexcercise.infra.pokeapi;

import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonDetail;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;
import com.ballastlane.blaexcercise.domain.model.Stat;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.EvolutionChainResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonListResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonResponse;
import com.ballastlane.blaexcercise.infra.pokeapi.dto.PokemonSpeciesResponse;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Output adapter: implements PokemonCatalog using the public PokeAPI.
 * It translates PokeAPI responses into domain models; the HTTP calls live in PokeApiClient.
 */
public class PokeApiCatalogAdapter implements PokemonCatalog {

    private static final String ENGLISH = "en";
    private static final double HECTOGRAMS_PER_KILOGRAM = 10.0;

    private final PokeApiClient pokeApi;

    public PokeApiCatalogAdapter(PokeApiClient pokeApi) {
        this.pokeApi = pokeApi;
    }

    @Override
    public PageResult<PokemonSummary> findAll(int page, int size) {
        int offset = page * size;

        PokemonListResponse list = pokeApi.getPokemonPage(offset, size);

        List<PokemonSummary> items = list.results().stream()
                .map(resource -> toSummary(resource.name()))
                .toList();

        return new PageResult<>(items, page, size, list.count());
    }

    private PokemonSummary toSummary(String name) {
        PokemonResponse pokemon = pokeApi.getPokemon(name);
        PokemonSpeciesResponse species = pokeApi.getSpecies(extractId(pokemon.species().url()));

        return new PokemonSummary(
                pokemon.id(),
                pokemon.name(),
                pokemon.sprites().frontDefault(),
                englishCategory(species),
                pokemon.weight() / HECTOGRAMS_PER_KILOGRAM,
                abilityNames(pokemon));
    }

    @Override
    public Optional<PokemonDetail> findByIdOrName(String idOrName) {
        return fetchPokemonIfExists(idOrName).map(this::toDetail);
    }

    private Optional<PokemonResponse> fetchPokemonIfExists(String idOrName) {
        try {
            return Optional.ofNullable(pokeApi.getPokemon(idOrName));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    private PokemonDetail toDetail(PokemonResponse pokemon) {
        PokemonSpeciesResponse species = pokeApi.getSpecies(extractId(pokemon.species().url()));
        EvolutionChainResponse evolution = pokeApi.getEvolutionChain(extractId(species.evolutionChain().url()));

        return new PokemonDetail(
                pokemon.id(),
                pokemon.name(),
                officialArtwork(pokemon),
                stats(pokemon),
                englishDescription(species),
                evolutionNames(evolution.chain()));
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
