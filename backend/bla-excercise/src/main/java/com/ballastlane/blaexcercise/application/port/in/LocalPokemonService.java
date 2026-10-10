package com.ballastlane.blaexcercise.application.port.in;

import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.domain.model.PageResult;

import java.util.Set;

/**
 * Input port: read, edit (US04) and delete the Pokemon stored locally.
 * Creating them is SyncPokemonService (US03).
 */
public interface LocalPokemonService {

    PageResult<LocalPokemon> list(int page, int size);

    LocalPokemon get(long id);

    /**
     * Replaces our own fields (localized name, region, tags). Official data is never changed.
     */
    LocalPokemon updateCustomFields(long id, String localizedName, String region, Set<String> tags);

    void delete(long id);
}
