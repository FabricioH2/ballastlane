package com.genai.tasks.user.infrastructure.config;

import com.genai.tasks.user.application.AuthenticateUserUseCase;
import com.genai.tasks.user.application.port.out.PasswordVerifier;
import com.genai.tasks.user.application.port.out.TokenIssuer;
import com.genai.tasks.user.application.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class UserUseCaseConfig {

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(UserRepository users, PasswordVerifier passwordVerifier,
                                                    TokenIssuer tokenIssuer) {
        return new AuthenticateUserUseCase(users, passwordVerifier, tokenIssuer);
    }
}
