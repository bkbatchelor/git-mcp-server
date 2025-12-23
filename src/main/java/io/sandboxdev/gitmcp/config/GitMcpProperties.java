package io.sandboxdev.gitmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for the Git MCP Server.
 * 
 * This record defines type-safe configuration properties that can be externalized
 * via application.yml or environment variables. All properties are validated
 * at startup to ensure fail-fast behavior.
 */
@ConfigurationProperties(prefix = "git.mcp")
@Validated
public record GitMcpProperties(
    @Valid @NotNull TransportConfig transport,
    @Valid @NotNull SecurityConfig security,
    @Valid @NotNull RepositoryConfig repository,
    @Valid @NotNull ObservabilityConfig observability
) {
    
    /**
     * Transport layer configuration for MCP communication.
     */
    public record TransportConfig(
        boolean stdioEnabled,
        boolean sseEnabled,
        @Min(1024) @Max(65535) int ssePort,
        @NotNull Duration requestTimeout
    ) {}
    
    /**
     * Security configuration for input validation and access control.
     */
    public record SecurityConfig(
        @NotEmpty List<String> allowedRepositories,
        boolean enableInputSanitization,
        @Min(1) @Max(100) int maxConcurrentOperations
    ) {}
    
    /**
     * Git repository configuration.
     */
    public record RepositoryConfig(
        @NotNull String defaultBranch,
        @NotNull String maxFileSize
    ) {}
    
    /**
     * Observability configuration for logging, tracing, and metrics.
     */
    public record ObservabilityConfig(
        boolean tracingEnabled,
        boolean metricsEnabled,
        @NotNull String logLevel
    ) {}
}