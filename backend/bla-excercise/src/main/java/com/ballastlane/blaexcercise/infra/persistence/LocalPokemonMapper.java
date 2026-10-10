package com.ballastlane.blaexcercise.infra.persistence;

import com.ballastlane.blaexcercise.domain.model.LocalPokemon;

import java.util.HashSet;

/**
 * Converts between the domain record and the JPA entity.
 */
final class LocalPokemonMapper {

    private LocalPokemonMapper() {
    }

    static LocalPokemonEntity toEntity(LocalPokemon pokemon) {
        LocalPokemonEntity entity = new LocalPokemonEntity();
        entity.setId(pokemon.id());
        entity.setPokeapiId(pokemon.pokeapiId());
        entity.setName(pokemon.name());
        entity.setImageUrl(pokemon.imageUrl());
        entity.setDescription(pokemon.description());
        entity.setLocalizedName(pokemon.localizedName());
        entity.setRegion(pokemon.region());
        entity.setTags(new HashSet<>(pokemon.tags()));
        return entity;
    }

    static LocalPokemon toDomain(LocalPokemonEntity entity) {
        return new LocalPokemon(
                entity.getId(),
                entity.getPokeapiId(),
                entity.getName(),
                entity.getImageUrl(),
                entity.getDescription(),
                entity.getLocalizedName(),
                entity.getRegion(),
                entity.getTags(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
