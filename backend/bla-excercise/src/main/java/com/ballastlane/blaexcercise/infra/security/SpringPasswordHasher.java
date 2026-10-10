package com.ballastlane.blaexcercise.infra.security;

import com.ballastlane.blaexcercise.application.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Output adapter: implements PasswordHasher with Spring Security's PasswordEncoder (BCrypt).
 */
public class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public SpringPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
