package com.genai.tasks.shared.web;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.UUID;

/**
 * Resolves the caller's user id from the access token. This is the only source of a task's owner id.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new InvalidBearerTokenException("Token has no subject");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new InvalidBearerTokenException("Token subject is not a user id");
        }
    }
}
