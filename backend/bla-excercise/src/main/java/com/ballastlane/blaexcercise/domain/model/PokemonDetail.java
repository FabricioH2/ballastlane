package com.ballastlane.blaexcercise.domain.model;

import java.util.List;

public record PokemonDetail(int id,
                            String name,
                            String imageUrl,
                            List<Stat> stats,
                            String description,
                            List<String> evolutionChain) {

    public PokemonDetail {
        stats = stats == null ? List.of() : List.copyOf(stats);
        evolutionChain = evolutionChain == null ? List.of() : List.copyOf(evolutionChain);
    }
}
