package com.ballastlane.blaexcercise.domain.exception;

public class LocalPokemonNotFoundException extends RuntimeException {

    public LocalPokemonNotFoundException(long id) {
        super("Local pokemon not found: " + id);
    }
}
