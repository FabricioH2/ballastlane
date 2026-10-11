package com.genai.tasks.user.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BCryptPasswordVerifierTest {

    private final BCryptPasswordEncoder realEncoder = new BCryptPasswordEncoder();
    private PasswordEncoder encoder;
    private BCryptPasswordVerifier verifier;

    @BeforeEach
    void setUp() {
        encoder = spy(realEncoder);
        verifier = new BCryptPasswordVerifier(encoder);
        clearInvocations(encoder);
    }

    @Test
    void dummyCheckRunsOneRealBCryptComparisonWithTheSameCostFactor() {
        String costPrefix = realEncoder.encode("any").substring(0, 7); // e.g. "$2a$10$"

        verifier.verifyAgainstDummyHash("guessed password");

        verify(encoder, times(1)).matches(eq("guessed password"),
                argThat(hash -> hash != null && hash.startsWith(costPrefix) && hash.length() == 60));
    }

    @Test
    void matchesDelegatesToTheEncoder() {
        String hash = realEncoder.encode("secret");

        assertThat(verifier.matches("secret", hash)).isTrue();
        assertThat(verifier.matches("wrong", hash)).isFalse();
    }
}
