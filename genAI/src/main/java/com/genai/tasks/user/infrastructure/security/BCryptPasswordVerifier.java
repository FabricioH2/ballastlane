package com.genai.tasks.user.infrastructure.security;

import com.genai.tasks.user.application.port.out.PasswordVerifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class BCryptPasswordVerifier implements PasswordVerifier {

    private final PasswordEncoder passwordEncoder;

    /**
     * Hash of a random throwaway password, made with the same encoder (and therefore the same cost factor)
     * as real hashes, so checking against it takes as long as checking a real user's password.
     */
    private final String dummyHash;

    BCryptPasswordVerifier(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    @Override
    public void verifyAgainstDummyHash(String rawPassword) {
        passwordEncoder.matches(rawPassword, dummyHash);
    }
}
