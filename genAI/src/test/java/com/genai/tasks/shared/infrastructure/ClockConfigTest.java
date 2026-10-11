package com.genai.tasks.shared.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(ClockConfig.class);

    @Test
    void defaultsToUtc() {
        context.run(ctx -> assertThat(ctx.getBean(Clock.class).getZone().normalized()).isEqualTo(ZoneOffset.UTC));
    }

    @Test
    void usesConfiguredBusinessTimeZone() {
        context.withPropertyValues("app.time-zone=America/Guayaquil")
                .run(ctx -> assertThat(ctx.getBean(Clock.class).getZone())
                        .isEqualTo(ZoneId.of("America/Guayaquil")));
    }

    @Test
    void refusesToStartWithAnUnknownTimeZone() {
        context.withPropertyValues("app.time-zone=Mars/Olympus_Mons")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
