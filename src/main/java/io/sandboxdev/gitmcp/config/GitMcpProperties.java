package io.sandboxdev.gitmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Type-safe configuration properties for Git MCP Server.
 * Requirement 15.1: Use @ConfigurationProperties for type-safe configuration
 */
@ConfigurationProperties(prefix = "git.mcp")
@Validated
public record GitMcpProperties(
    @Valid @NotNull TransportConfig transport,
    @Valid @NotNull SecurityConfig security,
    @Valid @NotNull RepositoryConfig repository,
    @Valid @NotNull HeadlessConfig headless,
    @Valid @NotNull ObservabilityConfig observability
) {

    /**
     * Transport layer configuration.
     */
    public record TransportConfig(
        boolean stdioEnabled,
        boolean sseEnabled,
        int ssePort,
        java.time.Duration requestTimeout
    ) {
        public TransportConfig {
            if (!stdioEnabled && !sseEnabled) {
                throw new IllegalArgumentException("At least one transport must be enabled");
            }
        }
    }

    /**
     * Security configuration.
     */
    public record SecurityConfig(
        @NotNull List<String> allowedRepositories,
        boolean enableInputSanitization,
        int maxRequestsPerMinute,
        int maxConcurrentOperations
    ) {}

    /**
     * Repository configuration.
     */
    public record RepositoryConfig(
        @NotNull String defaultBranch,
        boolean autoFetch,
        int operationTimeoutSeconds,
        @NotNull String maxFileSize
    ) {}

    /**
     * Headless deployment configuration.
     */
    public record HeadlessConfig(
        boolean daemonMode,
        boolean structuredLogging,
        int shutdownTimeoutSeconds,
        boolean batchProcessing
    ) {}

    /**
     * Observability configuration.
     */
    public record ObservabilityConfig(
        boolean tracingEnabled,
        boolean metricsEnabled,
        @NotNull String logLevel
    ) {}
}
