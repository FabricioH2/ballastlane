package com.ballastlane.blaexcercise.web.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/auth/login. No size rules here: the login should not reveal the registration rules.
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
