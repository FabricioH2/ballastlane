package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.in.PokemonDetailService;
import com.ballastlane.blaexcercise.application.port.out.PokemonCatalog;
import com.ballastlane.blaexcercise.domain.exception.InvalidPokemonIdentifierException;
import com.ballastlane.blaexcercise.domain.exception.PokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.model.PokemoDetail;

import java.util.Locale;

public class GetPokemonDetailServiceImpl implements PokemonDetailService {

    private final PokemonCatalog catalog;

    public GetPokemonDetailServiceImpl(PokemonCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PokemoDetail getDetail(String idOrName) {
        if (idOrName == null || idOrName.isBlank()) {
            throw new InvalidPokemonIdentifierException("Pokemon id or name must not be blank");
        }
        String normalized = idOrName.trim().toLowerCase(Locale.ROOT);

        return catalog.findByIdOrName(normalized)
                .orElseThrow(() -> new PokemonNotFoundException(normalized));
    }
}
