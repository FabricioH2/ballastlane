package com.ballastlane.blaexcercise.application.port.in;

import com.ballastlane.blaexcercise.domain.model.LocalPokemon;

/**
 * US03: copy a Pokemon from the catalog into our own database.
 */
public interface SyncPokemonService {
    LocalPokemon sync(String idOrName);
}
