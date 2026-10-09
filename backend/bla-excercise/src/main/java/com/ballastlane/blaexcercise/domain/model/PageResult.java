package com.ballastlane.blaexcercise.domain.model;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long totalElements) {

    public PageResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }
}
