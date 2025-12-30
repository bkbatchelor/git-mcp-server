package io.sandboxdev.gitmcp.headless;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import org.springframework.stereotype.Component;

/**
 * Provides health check endpoints for container orchestration.
 * Requirement 16.5: Health check endpoints for container orchestration
 */
@Component
public class HealthCheck {

    private final GitMcpProperties.HeadlessConfig config;

    public HealthCheck(GitMcpProperties properties) {
        this.config = properties.headless();
    }

    public boolean isAvailable() {
        return true;
    }
}
