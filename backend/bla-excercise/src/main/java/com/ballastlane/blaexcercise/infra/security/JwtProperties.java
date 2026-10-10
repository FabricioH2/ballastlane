package com.ballastlane.blaexcercise.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;

/**
 * Settings under "app.jwt" in application.yaml.
 *
 * @param secret     Base64 key used to sign and verify the tokens (HS256 needs at least 32 bytes)
 * @param expiration how long a token is valid
 * @param issuer     who created the token; tokens with another issuer are rejected
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("1h") Duration expiration,
        @DefaultValue("bla-excercise") String issuer) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        // Fail at startup instead of running with a weak or missing key.
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is required");
        }
        if (Base64.getDecoder().decode(secret).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be a Base64 value of at least 32 bytes (256 bits)");
        }
    }

    public SecretKey secretKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
    }
}
