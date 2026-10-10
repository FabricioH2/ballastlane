package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.out.PasswordHasher;
import com.ballastlane.blaexcercise.application.port.out.TokenIssuer;
import com.ballastlane.blaexcercise.application.port.out.UserRepository;
import com.ballastlane.blaexcercise.domain.exception.InvalidCredentialsException;
import com.ballastlane.blaexcercise.domain.model.AuthToken;
import com.ballastlane.blaexcercise.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private static final User ASH = new User(1L, "ash", "hashed-password", Instant.parse("2026-10-10T12:00:00Z"));

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final LoginServiceImpl service = new LoginServiceImpl(userRepository, passwordHasher, tokenIssuer);

    @Test
    void returnsTokenWhenCredentialsAreValid() {
        AuthToken token = new AuthToken("jwt-token", Instant.parse("2026-10-10T13:00:00Z"));
        when(userRepository.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(passwordHasher.matches("pikachu123", "hashed-password")).thenReturn(true);
        when(tokenIssuer.issueFor(ASH)).thenReturn(token);

        assertThat(service.login(" ASH ", "pikachu123")).isEqualTo(token);
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.findByUsername("misty")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("misty", "pikachu123"))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void rejectsWrongPassword() {
        when(userRepository.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(passwordHasher.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> service.login("ash", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(tokenIssuer);
    }
}
