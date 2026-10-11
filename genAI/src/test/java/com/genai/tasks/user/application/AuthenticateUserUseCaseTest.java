package com.genai.tasks.user.application;

import com.genai.tasks.user.application.port.out.PasswordVerifier;
import com.genai.tasks.user.application.port.out.TokenIssuer;
import com.genai.tasks.user.application.port.out.UserRepository;
import com.genai.tasks.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    private static final User ALICE = new User(UUID.randomUUID(), "alice", "$2a$10$hash");

    @Mock
    private UserRepository users;
    @Mock
    private PasswordVerifier passwordVerifier;
    @Mock
    private TokenIssuer tokenIssuer;

    private AuthenticateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateUserUseCase(users, passwordVerifier, tokenIssuer);
    }

    @Test
    void issuesTokenForValidCredentials() {
        IssuedToken token = new IssuedToken("jwt", Instant.parse("2026-10-11T11:00:00Z"));
        when(users.findByUsername("alice")).thenReturn(Optional.of(ALICE));
        when(passwordVerifier.matches("secret", ALICE.passwordHash())).thenReturn(true);
        when(tokenIssuer.issueFor(ALICE)).thenReturn(token);

        assertThat(useCase.execute("alice", "secret")).isSameAs(token);
    }

    @Test
    void rejectsWrongPasswordAfterExactlyOnePasswordCheck() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(ALICE));
        when(passwordVerifier.matches("wrong", ALICE.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("alice", "wrong")).isInstanceOf(InvalidCredentialsException.class);
        verify(passwordVerifier, times(1)).matches("wrong", ALICE.passwordHash());
        verify(passwordVerifier, never()).verifyAgainstDummyHash(any());
        verify(tokenIssuer, never()).issueFor(any());
    }

    @Test
    void unknownUserStillCostsExactlyOnePasswordCheck() {
        when(users.findByUsername("mallory")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("mallory", "x")).isInstanceOf(InvalidCredentialsException.class);
        verify(passwordVerifier, times(1)).verifyAgainstDummyHash("x");
        verify(passwordVerifier, never()).matches(any(), any());
        verify(tokenIssuer, never()).issueFor(any());
    }
}
