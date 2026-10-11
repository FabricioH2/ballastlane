package com.genai.tasks.user.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * An existing user account. Users are provisioned outside this API.
 */
public record User(UUID id, String username, String passwordHash) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordHash, "passwordHash");
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + "]";
    }
}
