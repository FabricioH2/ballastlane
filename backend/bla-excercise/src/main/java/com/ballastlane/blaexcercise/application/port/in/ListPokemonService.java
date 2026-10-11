package com.ballastlane.blaexcercise.application.port.in;

import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;

/**
 * US01: list Pokemon from the catalog, paginated.
 */
public interface ListPokemonService {
    PageResult<PokemonSummary> list(int page, int size);
}
