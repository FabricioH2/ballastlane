package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.in.ListPokemonUseCase;
import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.domain.exception.InvalidPaginationException;
import com.ballastlane.blaexcercise.domain.model.PageResult;
import com.ballastlane.blaexcercise.domain.model.PokemonSummary;

/**
 *
 * It is registered as a bean from infrastructure configuration.
 */
public class ListPokemonService implements ListPokemonUseCase {


    private final PokemonCatalog catalog;
    private final int maxPageSize;

    public ListPokemonService(PokemonCatalog catalog, int maxPageSize) {
        if (maxPageSize < 1) {
            throw new IllegalArgumentException("maxPageSize must be >= 1");
        }
        this.catalog = catalog;
        this.maxPageSize = maxPageSize;
    }

    @Override
    public PageResult<PokemonSummary> list(int page, int size) {
        if (page < 0) {
            throw new InvalidPaginationException("Page must be >= 0");
        }
        if (size < 1 || size > maxPageSize) {
            throw new InvalidPaginationException("Size must be between 1 and " + maxPageSize);
        }
        return catalog.findAll(page, size);
    }
}
