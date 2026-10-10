package com.ballastlane.blaexcercise.domain.exception;

public class InvalidPokemonIdentifierException extends RuntimeException {
    public InvalidPokemonIdentifierException(String message) {
        super(message);
    }
}
