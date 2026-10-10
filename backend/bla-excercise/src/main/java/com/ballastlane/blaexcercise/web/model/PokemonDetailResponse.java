package com.ballastlane.blaexcercise.web.model;

import com.ballastlane.blaexcercise.domain.model.PokemoDetail;
import com.ballastlane.blaexcercise.domain.model.Stat;

import java.util.List;

public record PokemonDetailResponse(int id,
                                    String name,
                                    String imageUrl,
                                    List<StatResponse> stats,
                                    String description,
                                    List<String> evolutionChain) {

    public record StatResponse(String name, int baseValue) {

        static StatResponse from(Stat stat) {
            return new StatResponse(stat.name(), stat.baseValue());
        }
    }

    public static PokemonDetailResponse from(PokemoDetail detail) {
        return new PokemonDetailResponse(
                detail.id(),
                detail.name(),
                detail.imageUrl(),
                detail.stats().stream().map(StatResponse::from).toList(),
                detail.description(),
                detail.evolutionChain());
    }
}
