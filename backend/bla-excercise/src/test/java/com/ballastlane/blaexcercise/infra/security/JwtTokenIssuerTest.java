package com.ballastlane.blaexcercise.infra.security;

import com.ballastlane.blaexcercise.domain.model.AuthToken;
import com.ballastlane.blaexcercise.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Plain unit test: uses the real encoder and decoder from SecurityConfig, without starting Spring.
 * It proves that a token we issue is accepted by the same checks the API applies to every request.
 */
class JwtTokenIssuerTest {

    private static final User ASH = new User(1L, "ash", "hashed-password", Instant.now());

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final JwtProperties properties = properties("0123456789abcdef0123456789abcdef");
    private final JwtTokenIssuer tokenIssuer = new JwtTokenIssuer(securityConfig.jwtEncoder(properties), properties);
    private final JwtDecoder jwtDecoder = securityConfig.jwtDecoder(properties);

    @Test
    void issuesTokenThatTheApiAccepts() {
        AuthToken token = tokenIssuer.issueFor(ASH);

        Jwt jwt = jwtDecoder.decode(token.accessToken());

        assertThat(jwt.getSubject()).isEqualTo("ash");
        assertThat(jwt.getClaimAsString(JwtClaimNames.ISS)).isEqualTo("bla-excercise-test");
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
        assertThat(token.expiresAt()).isCloseTo(Instant.now().plus(Duration.ofMinutes(30)), within(5, SECONDS));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtProperties otherProperties = properties("another-secret-another-secret-123");
        AuthToken foreignToken = new JwtTokenIssuer(securityConfig.jwtEncoder(otherProperties), otherProperties)
                .issueFor(ASH);

        assertThatThrownBy(() -> jwtDecoder.decode(foreignToken.accessToken()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void refusesToStartWithATooShortSecret() {
        assertThatThrownBy(() -> properties("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static JwtProperties properties(String rawSecret) {
        String secret = Base64.getEncoder().encodeToString(rawSecret.getBytes(StandardCharsets.UTF_8));
        return new JwtProperties(secret, Duration.ofMinutes(30), "bla-excercise-test");
    }
}
