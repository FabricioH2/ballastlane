package com.genai.tasks.shared.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
class ClockConfig {

    /**
     * The clock's zone is the business time zone: it decides what "today" means for due-date validation.
     * Instants (createdAt, token expiry) are unaffected. An unknown zone id fails startup.
     */
    @Bean
    Clock clock(@Value("${app.time-zone:UTC}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
