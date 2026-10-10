package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.in.PokemonDetailService;
import com.ballastlane.blaexcercise.application.port.in.SyncPokemonService;
import com.ballastlane.blaexcercise.application.port.out.LocalPokemonRepository;
import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.domain.model.PokemonDetail;

public class SyncPokemonServiceImpl implements SyncPokemonService {

    private final PokemonDetailService pokemonDetailService;
    private final LocalPokemonRepository repository;

    public SyncPokemonServiceImpl(PokemonDetailService pokemonDetailService,
                                  LocalPokemonRepository repository) {
        this.pokemonDetailService = pokemonDetailService;
        this.repository = repository;
    }

    @Override
    public LocalPokemon sync(String idOrName) {
        PokemonDetail detail = pokemonDetailService.getDetail(idOrName);

        if (repository.existsByPokeapiId(detail.id())) {
            throw new PokemonAlreadySyncedException(detail.id());
        }

        return repository.save(LocalPokemon.newFrom(detail));
    }
}
