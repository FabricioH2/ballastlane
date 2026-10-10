package com.ballastlane.blaexcercise.domain.exception;

public class PokemonCatalogUnavailableException extends RuntimeException {
    public PokemonCatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
