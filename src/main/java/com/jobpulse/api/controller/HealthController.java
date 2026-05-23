package com.jobpulse.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Health check endpoint for monitoring application status.
 * Used by load balancers, monitoring systems, and Docker health checks.
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Application health and status monitoring")
public class HealthController {

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    @Value("${spring.application.name:JobPulse Auto Apply}")
    private String applicationName;

    /**
     * Health check endpoint
     * Returns 200 OK if the application is healthy
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Returns application status")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(
            HealthResponse.builder()
                .status("UP")
                .applicationName(applicationName)
                .version(applicationVersion)
                .timestamp(Instant.now())
                .message("Application is running")
                .build()
        );
    }

    /**
     * Detailed health status
     */
    @GetMapping("/detailed")
    @Operation(summary = "Detailed health status", description = "Returns comprehensive application health information")
    public ResponseEntity<DetailedHealthResponse> detailedHealth() {
        return ResponseEntity.ok(
            DetailedHealthResponse.builder()
                .status("UP")
                .applicationName(applicationName)
                .version(applicationVersion)
                .timestamp(Instant.now())
                .uptime(Runtime.getRuntime().totalMemory())
                .memoryUsage(DetailedHealthResponse.MemoryInfo.builder()
                    .totalMemory(Runtime.getRuntime().totalMemory())
                    .freeMemory(Runtime.getRuntime().freeMemory())
                    .usedMemory(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                    .maxMemory(Runtime.getRuntime().maxMemory())
                    .build())
                .build()
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthResponse {
        private String status;
        private String applicationName;
        private String version;
        private Instant timestamp;
        private String message;

        public static HealthResponseBuilder builder() {
            return new HealthResponseBuilder();
        }

        public static class HealthResponseBuilder {
            private String status;
            private String applicationName;
            private String version;
            private Instant timestamp;
            private String message;

            public HealthResponseBuilder status(String status) {
                this.status = status;
                return this;
            }

            public HealthResponseBuilder applicationName(String applicationName) {
                this.applicationName = applicationName;
                return this;
            }

            public HealthResponseBuilder version(String version) {
                this.version = version;
                return this;
            }

            public HealthResponseBuilder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public HealthResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public HealthResponse build() {
                return new HealthResponse(status, applicationName, version, timestamp, message);
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailedHealthResponse {
        private String status;
        private String applicationName;
        private String version;
        private Instant timestamp;
        private long uptime;
        private MemoryInfo memoryUsage;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MemoryInfo {
            private long totalMemory;
            private long freeMemory;
            private long usedMemory;
            private long maxMemory;

            public static MemoryInfoBuilder builder() {
                return new MemoryInfoBuilder();
            }

            public static class MemoryInfoBuilder {
                private long totalMemory;
                private long freeMemory;
                private long usedMemory;
                private long maxMemory;

                public MemoryInfoBuilder totalMemory(long totalMemory) {
                    this.totalMemory = totalMemory;
                    return this;
                }

                public MemoryInfoBuilder freeMemory(long freeMemory) {
                    this.freeMemory = freeMemory;
                    return this;
                }

                public MemoryInfoBuilder usedMemory(long usedMemory) {
                    this.usedMemory = usedMemory;
                    return this;
                }

                public MemoryInfoBuilder maxMemory(long maxMemory) {
                    this.maxMemory = maxMemory;
                    return this;
                }

                public MemoryInfo build() {
                    return new MemoryInfo(totalMemory, freeMemory, usedMemory, maxMemory);
                }
            }
        }

        public static DetailedHealthResponseBuilder builder() {
            return new DetailedHealthResponseBuilder();
        }

        public static class DetailedHealthResponseBuilder {
            private String status;
            private String applicationName;
            private String version;
            private Instant timestamp;
            private long uptime;
            private MemoryInfo memoryUsage;

            public DetailedHealthResponseBuilder status(String status) {
                this.status = status;
                return this;
            }

            public DetailedHealthResponseBuilder applicationName(String applicationName) {
                this.applicationName = applicationName;
                return this;
            }

            public DetailedHealthResponseBuilder version(String version) {
                this.version = version;
                return this;
            }

            public DetailedHealthResponseBuilder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public DetailedHealthResponseBuilder uptime(long uptime) {
                this.uptime = uptime;
                return this;
            }

            public DetailedHealthResponseBuilder memoryUsage(MemoryInfo memoryUsage) {
                this.memoryUsage = memoryUsage;
                return this;
            }

            public DetailedHealthResponse build() {
                return new DetailedHealthResponse(status, applicationName, version, timestamp, uptime, memoryUsage);
            }
        }
    }
}

