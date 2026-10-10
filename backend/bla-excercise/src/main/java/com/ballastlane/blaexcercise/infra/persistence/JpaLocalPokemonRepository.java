package com.ballastlane.blaexcercise.infra.persistence;

import com.ballastlane.blaexcercise.application.port.out.LocalPokemonRepository;
import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Output adapter: implements the LocalPokemonRepository port with Spring Data JPA.
 */
public class JpaLocalPokemonRepository implements LocalPokemonRepository {

    private final SpringDataLocalPokemonRepository jpaRepository;

    public JpaLocalPokemonRepository(SpringDataLocalPokemonRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LocalPokemon save(LocalPokemon pokemon) {
        try {
            LocalPokemonEntity saved = jpaRepository.saveAndFlush(LocalPokemonMapper.toEntity(pokemon));
            return LocalPokemonMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            // The unique constraint on pokeapi_id protects us when two requests sync the same Pokemon at once.
            throw new PokemonAlreadySyncedException(pokemon.pokeapiId());
        }
    }

    @Override
    public boolean existsByPokeapiId(int pokeapiId) {
        return jpaRepository.existsByPokeapiId(pokeapiId);
    }
}
