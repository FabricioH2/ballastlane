package com.ballastlane.blaexcercise.domain.model;

import java.time.Instant;
import java.util.Locale;

/**
 * A user of our API. The password is never stored: only its hash.
 */
public record User(Long id, String username, String passwordHash, Instant createdAt) {

    /**
     * A user that does not exist in the database yet (no id, no creation date).
     */
    public static User newUser(String username, String passwordHash) {
        return new User(null, username, passwordHash, null);
    }

    /**
     * Usernames are case-insensitive: "Ash", " ash " and "ASH" are the same user.
     */
    public static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Keeps the password hash out of logs and error messages.
     */
    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", createdAt=" + createdAt + "]";
    }
}
