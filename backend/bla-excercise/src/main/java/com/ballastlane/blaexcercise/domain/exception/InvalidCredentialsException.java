package com.ballastlane.blaexcercise.domain.exception;

/**
 * Same message for "user does not exist" and "wrong password",
 * so nobody can use the login to find out which usernames exist.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
