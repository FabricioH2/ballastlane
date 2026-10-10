package com.ballastlane.blaexcercise.application.port.in;

import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;

/**
 * Input port : list Pokemon with pagination.
 */
public interface ListPokemonService {
    PageResult<PokemonSummary> list(int page, int size);
}
