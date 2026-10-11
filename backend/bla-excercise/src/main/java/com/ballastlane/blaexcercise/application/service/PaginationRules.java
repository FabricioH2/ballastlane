package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.domain.exception.InvalidPaginationException;

/**
 * Pagination rules shared by every list use case (PokeAPI list and local list).
 */
final class PaginationRules {

    private PaginationRules() {
    }

    static int requireValidMaxPageSize(int maxPageSize) {
        if (maxPageSize < 1) {
            throw new IllegalArgumentException("maxPageSize must be >= 1");
        }
        return maxPageSize;
    }

    static void validate(int page, int size, int maxPageSize) {
        if (page < 0) {
            throw new InvalidPaginationException("Page must be >= 0");
        }
        if (size < 1 || size > maxPageSize) {
            throw new InvalidPaginationException("Size must be between 1 and " + maxPageSize);
        }
    }
}
