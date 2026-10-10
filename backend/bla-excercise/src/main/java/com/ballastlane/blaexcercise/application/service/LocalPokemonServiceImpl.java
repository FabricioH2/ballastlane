package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.in.LocalPokemonService;
import com.ballastlane.blaexcercise.application.port.out.LocalPokemonRepository;
import com.ballastlane.blaexcercise.domain.exception.LocalPokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.domain.model.PageResult;

import java.util.Set;

public class LocalPokemonServiceImpl implements LocalPokemonService {

    private final LocalPokemonRepository repository;
    private final int maxPageSize;

    public LocalPokemonServiceImpl(LocalPokemonRepository repository, int maxPageSize) {
        this.repository = repository;
        this.maxPageSize = PaginationRules.requireValidMaxPageSize(maxPageSize);
    }

    @Override
    public PageResult<LocalPokemon> list(int page, int size) {
        PaginationRules.validate(page, size, maxPageSize);
        return repository.findAll(page, size);
    }

    @Override
    public LocalPokemon get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new LocalPokemonNotFoundException(id));
    }

    @Override
    public LocalPokemon updateCustomFields(long id, String localizedName, String region, Set<String> tags) {
        LocalPokemon updated = get(id).withCustomFields(localizedName, region, tags);
        return repository.save(updated);
    }

    @Override
    public void delete(long id) {
        if (!repository.deleteById(id)) {
            throw new LocalPokemonNotFoundException(id);
        }
    }
}
