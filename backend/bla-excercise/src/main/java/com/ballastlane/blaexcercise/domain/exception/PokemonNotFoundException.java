package com.ballastlane.blaexcercise.domain.exception;

public class PokemonNotFoundException extends RuntimeException {
    public PokemonNotFoundException(String idOrName) {
        super("Pokemon not found: " + idOrName);
    }

}
