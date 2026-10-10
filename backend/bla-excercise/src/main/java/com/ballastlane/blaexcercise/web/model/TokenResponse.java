package com.ballastlane.blaexcercise.web.model;

import com.ballastlane.blaexcercise.domain.model.AuthToken;

import java.time.Instant;

/**
 * Login answer. The client sends it back as the header "Authorization: Bearer {accessToken}".
 */
public record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {

    private static final String BEARER = "Bearer";

    public static TokenResponse from(AuthToken token) {
        return new TokenResponse(token.accessToken(), BEARER, token.expiresAt());
    }
}
