package com.genai.tasks.user.application;

import com.genai.tasks.user.application.port.out.PasswordVerifier;
import com.genai.tasks.user.application.port.out.TokenIssuer;
import com.genai.tasks.user.application.port.out.UserRepository;
import com.genai.tasks.user.domain.User;

import java.util.Optional;

public class AuthenticateUserUseCase {

    private final UserRepository users;
    private final PasswordVerifier passwordVerifier;
    private final TokenIssuer tokenIssuer;

    public AuthenticateUserUseCase(UserRepository users, PasswordVerifier passwordVerifier, TokenIssuer tokenIssuer) {
        this.users = users;
        this.passwordVerifier = passwordVerifier;
        this.tokenIssuer = tokenIssuer;
    }

    /**
     * Exactly one password check runs whether or not the username exists, so response time does not
     * reveal which usernames are registered.
     */
    public IssuedToken execute(String username, String password) {
        Optional<User> user = users.findByUsername(username);
        if (user.isEmpty()) {
            passwordVerifier.verifyAgainstDummyHash(password);
            throw new InvalidCredentialsException();
        }
        if (!passwordVerifier.matches(password, user.get().passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenIssuer.issueFor(user.get());
    }
}
