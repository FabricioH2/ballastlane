package com.ballastlane.blaexcercise.infra.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on Spring's cache support. The cache provider (Caffeine) and its settings
 * come from application.yaml (spring.cache.*).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
