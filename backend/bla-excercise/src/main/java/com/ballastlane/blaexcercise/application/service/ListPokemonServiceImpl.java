package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.in.ListPokemonService;
import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;

/**
 * US01: list Pokemon from the PokeAPI catalog, with pagination.
 */
public class ListPokemonServiceImpl implements ListPokemonService {

    private final PokemonCatalog catalog;
    private final int maxPageSize;

    public ListPokemonServiceImpl(PokemonCatalog catalog, int maxPageSize) {
        this.catalog = catalog;
        this.maxPageSize = PaginationRules.requireValidMaxPageSize(maxPageSize);
    }

    @Override
    public PageResult<PokemonSummary> list(int page, int size) {
        PaginationRules.validate(page, size, maxPageSize);
        return catalog.findAll(page, size);
    }
}
