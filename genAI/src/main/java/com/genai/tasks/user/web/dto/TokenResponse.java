package com.genai.tasks.user.web.dto;

import java.time.Instant;

public record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {

    public static TokenResponse bearer(String accessToken, Instant expiresAt) {
        return new TokenResponse(accessToken, "Bearer", expiresAt);
    }

    @Override
    public String toString() {
        return "TokenResponse[tokenType=" + tokenType + ", expiresAt=" + expiresAt + "]";
    }
}
