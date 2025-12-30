package io.sandboxdev.gitmcp.headless;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import org.springframework.stereotype.Component;

/**
 * Provides structured logging for headless deployment.
 * Requirement 16.4: Structured JSON logging for log aggregation systems
 */
@Component
public class StructuredLogger {

    private final GitMcpProperties.HeadlessConfig config;

    public StructuredLogger(GitMcpProperties properties) {
        this.config = properties.headless();
    }

    public boolean isStructuredFormat() {
        return config.structuredLogging();
    }
}
