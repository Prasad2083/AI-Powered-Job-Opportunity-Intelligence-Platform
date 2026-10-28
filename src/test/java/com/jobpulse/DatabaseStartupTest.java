package com.jobpulse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
class DatabaseStartupTest {
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    static {
        postgres.start();
    }
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.security.jwt.secret", () -> "test-only-signing-key-that-is-long-enough-for-hs512-keep-out-of-production-123456789");
        registry.add("jobpulse.feeds.include-mock", () -> "false");
    }
    @Test void migrationsAndHibernateValidationStart() {}
}
