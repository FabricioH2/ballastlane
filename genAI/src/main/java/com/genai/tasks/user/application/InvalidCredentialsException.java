package com.genai.tasks.user.application;

/**
 * Unknown username or wrong password. The two cases are deliberately indistinguishable.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
