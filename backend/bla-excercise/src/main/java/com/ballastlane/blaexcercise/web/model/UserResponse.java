package com.ballastlane.blaexcercise.web.model;

import com.ballastlane.blaexcercise.domain.model.User;

import java.time.Instant;

/**
 * What the API returns about a user. The password hash is never sent.
 */
public record UserResponse(Long id, String username, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.createdAt());
    }
}
