package com.genai.tasks.user.web;

import com.genai.tasks.user.application.AuthenticateUserUseCase;
import com.genai.tasks.user.application.IssuedToken;
import com.genai.tasks.user.web.dto.TokenRequest;
import com.genai.tasks.user.web.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthenticateUserUseCase authenticateUser;

    AuthController(AuthenticateUserUseCase authenticateUser) {
        this.authenticateUser = authenticateUser;
    }

    @PostMapping("/token")
    TokenResponse token(@Valid @RequestBody TokenRequest request) {
        IssuedToken token = authenticateUser.execute(request.username(), request.password());
        return TokenResponse.bearer(token.value(), token.expiresAt());
    }
}
