package com.ballastlane.blaexcercise.domain.exception;

public class PokemonAlreadySyncedException extends RuntimeException {
    public PokemonAlreadySyncedException(int pokeapiId) {
        super("Pokemon already synced: " + pokeapiId);
    }
}
