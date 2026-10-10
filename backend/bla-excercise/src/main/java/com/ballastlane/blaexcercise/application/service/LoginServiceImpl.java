package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.in.LoginService;
import com.ballastlane.blaexcercise.application.port.out.PasswordHasher;
import com.ballastlane.blaexcercise.application.port.out.TokenIssuer;
import com.ballastlane.blaexcercise.application.port.out.UserRepository;
import com.ballastlane.blaexcercise.domain.exception.InvalidCredentialsException;
import com.ballastlane.blaexcercise.domain.model.AuthToken;
import com.ballastlane.blaexcercise.domain.model.User;

public class LoginServiceImpl implements LoginService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public LoginServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public AuthToken login(String username, String rawPassword) {
        User user = userRepository.findByUsername(User.normalizeUsername(username))
                .filter(found -> passwordHasher.matches(rawPassword, found.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return tokenIssuer.issueFor(user);
    }
}
