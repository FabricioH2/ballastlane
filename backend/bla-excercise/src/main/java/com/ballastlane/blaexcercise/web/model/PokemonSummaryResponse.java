package com.ballastlane.blaexcercise.web.model;

import com.ballastlane.blaexcercise.domain.model.PokemonSummary;

import java.util.List;

public record PokemonSummaryResponse(int id,
                                     String name,
                                     String spriteUrl,
                                     String category,
                                     double weightKg,
                                     List<String> abilities) {

    public static PokemonSummaryResponse from(PokemonSummary pokemon) {
        return new PokemonSummaryResponse(
                pokemon.id(),
                pokemon.name(),
                pokemon.spriteUrl(),
                pokemon.category(),
                pokemon.weight(),
                pokemon.abilities());
    }

}
