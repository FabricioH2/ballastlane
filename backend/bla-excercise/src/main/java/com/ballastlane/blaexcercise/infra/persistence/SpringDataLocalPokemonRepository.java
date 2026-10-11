package com.ballastlane.blaexcercise.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLocalPokemonRepository extends JpaRepository<LocalPokemonEntity, Long> {

    boolean existsByPokeapiId(int pokeapiId);
}
