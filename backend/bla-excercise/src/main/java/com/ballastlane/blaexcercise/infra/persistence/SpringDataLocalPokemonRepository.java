package com.ballastlane.blaexcercise.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data generates the implementation at runtime (save, findById, delete...).
 * existsByPokeapiId is a "derived query": Spring builds the SQL from the method name.
 */
public interface SpringDataLocalPokemonRepository extends JpaRepository<LocalPokemonEntity, Long> {

    boolean existsByPokeapiId(int pokeapiId);
}
