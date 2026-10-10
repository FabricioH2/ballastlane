package com.ballastlane.blaexcercise.domain.model;

import java.time.Instant;

/**
 * The result of a successful login: the token the client sends in every request, and when it stops working.
 */
public record AuthToken(String accessToken, Instant expiresAt) {
}
