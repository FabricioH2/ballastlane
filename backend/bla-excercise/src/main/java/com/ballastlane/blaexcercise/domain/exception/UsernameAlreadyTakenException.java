package com.ballastlane.blaexcercise.domain.exception;

public class UsernameAlreadyTakenException extends RuntimeException {

    public UsernameAlreadyTakenException(String username) {
        super("Username already taken: " + username);
    }
}
