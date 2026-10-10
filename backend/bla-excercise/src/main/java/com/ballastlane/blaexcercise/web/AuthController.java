package com.ballastlane.blaexcercise.web;

import com.ballastlane.blaexcercise.application.port.in.LoginService;
import com.ballastlane.blaexcercise.application.port.in.RegisterUserService;
import com.ballastlane.blaexcercise.web.model.LoginRequest;
import com.ballastlane.blaexcercise.web.model.RegisterRequest;
import com.ballastlane.blaexcercise.web.model.TokenResponse;
import com.ballastlane.blaexcercise.web.model.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserService registerUserService;
    private final LoginService loginService;

    public AuthController(RegisterUserService registerUserService, LoginService loginService) {
        this.registerUserService = registerUserService;
        this.loginService = loginService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(registerUserService.register(request.username(), request.password()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(loginService.login(request.username(), request.password()));
    }
}
