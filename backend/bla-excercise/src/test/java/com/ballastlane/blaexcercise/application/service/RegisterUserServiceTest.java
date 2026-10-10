package com.ballastlane.blaexcercise.application.service;

import com.ballastlane.blaexcercise.application.port.out.PasswordHasher;
import com.ballastlane.blaexcercise.application.port.out.UserRepository;
import com.ballastlane.blaexcercise.domain.exception.UsernameAlreadyTakenException;
import com.ballastlane.blaexcercise.domain.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final RegisterUserServiceImpl service = new RegisterUserServiceImpl(userRepository, passwordHasher);

    @Test
    void savesUserWithHashedPasswordAndNormalizedUsername() {
        when(userRepository.existsByUsername("ash")).thenReturn(false);
        when(passwordHasher.hash("pikachu123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new User(1L, user.username(), user.passwordHash(), Instant.now());
        });

        User result = service.register("  Ash ", "pikachu123");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().username()).isEqualTo("ash");
        assertThat(saved.getValue().passwordHash()).isEqualTo("hashed-password"); // never the raw password
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void rejectsUsernameThatIsAlreadyTaken() {
        when(userRepository.existsByUsername("ash")).thenReturn(true);

        assertThatThrownBy(() -> service.register("ash", "pikachu123"))
                .isInstanceOf(UsernameAlreadyTakenException.class);
        verify(userRepository, never()).save(any());
    }
}
