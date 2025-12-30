package io.sandboxdev.gitmcp.properties;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import io.sandboxdev.gitmcp.headless.GuiDependencyChecker;
import io.sandboxdev.gitmcp.headless.HealthCheck;
import io.sandboxdev.gitmcp.headless.ShutdownHandler;
import io.sandboxdev.gitmcp.headless.StructuredLogger;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotEmpty;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for headless deployment capabilities.
 */
class HeadlessDeploymentProperties {

    /**
     * Property 17: Headless Deployment (Req 16.1, 16.3, 16.4, 16.5, 16.7)
     * Validates headless operation capabilities including graceful shutdown,
     * daemon mode, structured logging, health checks, and zero GUI dependencies.
     */
    @Property
    void headlessDeploymentCapabilities(
        @ForAll boolean daemonMode,
        @ForAll boolean structuredLogging,
        @ForAll @IntRange(min = 1, max = 300) int shutdownTimeout,
        @ForAll boolean batchProcessing
    ) {
        var headlessConfig = new GitMcpProperties.HeadlessConfig(
            daemonMode, structuredLogging, shutdownTimeout, batchProcessing
        );

        var properties = createValidGitMcpProperties(headlessConfig);

        // Test graceful shutdown handling (Req 16.1)
        var shutdownHandler = new ShutdownHandler(properties);
        assertThat(shutdownHandler.supportsGracefulShutdown()).isTrue();

        // Test daemon mode operation (Req 16.3)
        if (daemonMode) {
            assertThat(shutdownHandler.isDaemonMode()).isTrue();
        }

        // Test structured logging (Req 16.4)
        if (structuredLogging) {
            var logger = new StructuredLogger(properties);
            assertThat(logger.isStructuredFormat()).isTrue();
        }

        // Test health check endpoints (Req 16.5)
        var healthCheck = new HealthCheck(properties);
        assertThat(healthCheck.isAvailable()).isTrue();

        // Test zero GUI dependencies (Req 16.7)
        var guiChecker = new GuiDependencyChecker();
        assertThat(guiChecker.hasGuiDependencies()).isFalse();
    }

    /**
     * Property 17.1: Configuration Validation (Req 15.4)
     * Validates configuration validation with fail-fast behavior.
     */
    @Property
    void configurationValidation(
        @ForAll @NotEmpty String allowedRepo,
        @ForAll @IntRange(min = 1, max = 3600) int timeout
    ) {
        // Test valid configuration
        var validConfig = createValidGitMcpProperties(allowedRepo, timeout);
        assertThat(validConfig).isNotNull();

        // Test invalid configuration fails fast
        assertThatThrownBy(() -> createInvalidGitMcpProperties())
            .isInstanceOf(IllegalArgumentException.class);
    }

    // Helper methods
    private GitMcpProperties createValidGitMcpProperties(GitMcpProperties.HeadlessConfig headlessConfig) {
        return new GitMcpProperties(
            new GitMcpProperties.TransportConfig(true, false, 8080, Duration.ofSeconds(30)),
            new GitMcpProperties.SecurityConfig(List.of("/valid/repo"), true, 60, 10),
            new GitMcpProperties.RepositoryConfig("main", false, 30, "10MB"),
            headlessConfig,
            new GitMcpProperties.ObservabilityConfig(true, true, "INFO")
        );
    }

    private GitMcpProperties createValidGitMcpProperties(String allowedRepo, int timeout) {
        return new GitMcpProperties(
                new GitMcpProperties.TransportConfig(true, false, 8080, Duration.ofSeconds(30)),
                new GitMcpProperties.SecurityConfig(List.of(allowedRepo), true, 60, 10),
                new GitMcpProperties.RepositoryConfig("main", false, timeout, "10MB"),
                new GitMcpProperties.HeadlessConfig(false, false, 10, false),
                new GitMcpProperties.ObservabilityConfig(true, true, "INFO")
        );
    }

    private GitMcpProperties createInvalidGitMcpProperties() {
        // This should fail because both transports are disabled
        return new GitMcpProperties(
                new GitMcpProperties.TransportConfig(false, false, 8080, Duration.ofSeconds(30)),
                new GitMcpProperties.SecurityConfig(List.of("/valid/repo"), true, 60, 10),
                new GitMcpProperties.RepositoryConfig("main", false, 30, "10MB"),
                new GitMcpProperties.HeadlessConfig(false, false, 10, false),
                new GitMcpProperties.ObservabilityConfig(true, true, "INFO")
        );
    }
}
