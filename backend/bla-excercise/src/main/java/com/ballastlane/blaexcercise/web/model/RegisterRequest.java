package com.ballastlane.blaexcercise.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/auth/register. @Valid in the controller checks these rules before the use case runs.
 * Password max 72: BCrypt ignores everything after 72 bytes.
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "may only contain letters, digits, '.', '_' and '-'")
        String username,

        @NotBlank
        @Size(min = 8, max = 72)
        String password) {
}
