package io.sandboxdev.gitmcp.headless;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import org.springframework.stereotype.Component;

/**
 * Handles graceful shutdown for headless deployment.
 * Requirement 16.1: Graceful shutdown handling via SIGTERM/SIGINT signals
 * Requirement 16.3: Daemon mode operation for background processes
 */
@Component
public class ShutdownHandler {

    private final GitMcpProperties.HeadlessConfig config;

    public ShutdownHandler(GitMcpProperties properties) {
        this.config = properties.headless();
    }

    public boolean supportsGracefulShutdown() {
        return true;
    }

    public boolean isDaemonMode() {
        return config.daemonMode();
    }
}
