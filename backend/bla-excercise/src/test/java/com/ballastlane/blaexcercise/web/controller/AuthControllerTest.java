package com.ballastlane.blaexcercise.web.controller;

import com.ballastlane.blaexcercise.application.port.in.LoginService;
import com.ballastlane.blaexcercise.application.port.in.RegisterUserService;
import com.ballastlane.blaexcercise.domain.exception.InvalidCredentialsException;
import com.ballastlane.blaexcercise.domain.exception.UsernameAlreadyTakenException;
import com.ballastlane.blaexcercise.domain.model.AuthToken;
import com.ballastlane.blaexcercise.domain.model.User;
import com.ballastlane.blaexcercise.infra.security.SecurityConfig;
import com.ballastlane.blaexcercise.web.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No @WithMockUser here: register and login must work without being logged in.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String ASH_CREDENTIALS = """
            {"username": "ash", "password": "pikachu123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserService registerUserService;

    @MockitoBean
    private LoginService loginService;

    @Test
    void registerReturns201WithTheNewUserButNotThePassword() throws Exception {
        when(registerUserService.register("ash", "pikachu123"))
                .thenReturn(new User(1L, "ash", "hashed-password", Instant.parse("2026-10-10T12:00:00Z")));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(ASH_CREDENTIALS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("ash"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerReturns400WithTheInvalidFields() throws Exception {
        String invalid = """
                {"username": "a", "password": "short"}
                """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());
        verifyNoInteractions(registerUserService);
    }

    @Test
    void registerReturns409WhenUsernameIsTaken() throws Exception {
        when(registerUserService.register("ash", "pikachu123")).thenThrow(new UsernameAlreadyTakenException("ash"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(ASH_CREDENTIALS))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Username already taken"));
    }

    @Test
    void loginReturnsBearerToken() throws Exception {
        when(loginService.login("ash", "pikachu123"))
                .thenReturn(new AuthToken("jwt-token", Instant.parse("2026-10-10T13:00:00Z")));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(ASH_CREDENTIALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-10T13:00:00Z"));
    }

    @Test
    void loginReturns401WithWrongCredentials() throws Exception {
        when(loginService.login("ash", "pikachu123")).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(ASH_CREDENTIALS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid credentials"));
    }
}
