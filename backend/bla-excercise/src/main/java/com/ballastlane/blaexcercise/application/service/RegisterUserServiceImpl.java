package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.in.RegisterUserService;
import com.ballastlane.blaexcercise.application.port.out.PasswordHasher;
import com.ballastlane.blaexcercise.application.port.out.UserRepository;
import com.ballastlane.blaexcercise.domain.exception.UsernameAlreadyTakenException;
import com.ballastlane.blaexcercise.domain.model.User;

public class RegisterUserServiceImpl implements RegisterUserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User register(String username, String rawPassword) {
        String normalizedUsername = User.normalizeUsername(username);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new UsernameAlreadyTakenException(normalizedUsername);
        }
        return userRepository.save(User.newUser(normalizedUsername, passwordHasher.hash(rawPassword)));
    }
}
