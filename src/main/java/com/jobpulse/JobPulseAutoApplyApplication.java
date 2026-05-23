package com.jobpulse;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * JobPulse Auto Apply - AI-Powered Job Opportunity Intelligence Platform
 *
 * Main entry point for the Spring Boot application.
 * Enables asynchronous processing, scheduled tasks, and caching for optimal performance.
 *
 * @author AI Solutions Architect
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
@OpenAPIDefinition(
    info = @Info(
        title = "JobPulse Auto Apply API",
        version = "1.0.0",
        description = "AI-Powered Job Opportunity Intelligence Platform - Discover, analyze, and apply to job opportunities with AI-driven insights",
        contact = @Contact(
            name = "JobPulse Team",
            url = "https://jobpulse.com"
        ),
        license = @License(
            name = "Proprietary",
            url = "https://jobpulse.com/license"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Development Server"
        ),
        @Server(
            url = "https://api.jobpulse.com",
            description = "Production Server"
        )
    }
)
public class JobPulseAutoApplyApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobPulseAutoApplyApplication.class, args);
    }
}

