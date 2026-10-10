package com.ballastlane.blaexcercise.web.model;

import com.ballastlane.blaexcercise.domain.model.LocalPokemon;

import java.time.Instant;
import java.util.List;

public record LocalPokemonResponse(Long id,
                                   int pokeapiId,
                                   String name,
                                   String imageUrl,
                                   String description,
                                   String localizedName,
                                   String region,
                                   List<String> tags,
                                   Instant createdAt,
                                   Instant updatedAt) {

    public static LocalPokemonResponse from(LocalPokemon pokemon) {
        return new LocalPokemonResponse(
                pokemon.id(),
                pokemon.pokeapiId(),
                pokemon.name(),
                pokemon.imageUrl(),
                pokemon.description(),
                pokemon.localizedName(),
                pokemon.region(),
                pokemon.tags().stream().sorted().toList(),
                pokemon.createdAt(),
                pokemon.updatedAt());
    }
}
