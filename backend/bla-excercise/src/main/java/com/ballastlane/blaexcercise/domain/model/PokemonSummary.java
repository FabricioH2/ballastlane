package com.ballastlane.blaexcercise.domain.model;

import java.util.List;

public record PokemonSummary(
        int id,
        String name,
        String spriteUrl,
        String category,
        double weight,
        List<String> abilities
) {
    public PokemonSummary {
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
    }
}
