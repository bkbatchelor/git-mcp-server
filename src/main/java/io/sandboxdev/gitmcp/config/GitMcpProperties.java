package io.sandboxdev.gitmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for the Git MCP Server.
 * 
 * This record defines type-safe configuration properties that can be externalized
 * via application.yml or environment variables.
 */
@ConfigurationProperties(prefix = "git.mcp")
public record GitMcpProperties(
    TransportConfig transport,
    SecurityConfig security,
    RepositoryConfig repository,
    ObservabilityConfig observability
) {
    
    /**
     * Transport layer configuration for MCP communication.
     */
    public record TransportConfig(
        boolean stdioEnabled,
        boolean sseEnabled,
        int ssePort,
        Duration requestTimeout
    ) {}
    
    /**
     * Security configuration for input validation and access control.
     */
    public record SecurityConfig(
        List<String> allowedRepositories,
        boolean enableInputSanitization,
        int maxConcurrentOperations
    ) {}
    
    /**
     * Git repository configuration.
     */
    public record RepositoryConfig(
        String defaultBranch,
        String maxFileSize
    ) {}
    
    /**
     * Observability configuration for logging, tracing, and metrics.
     */
    public record ObservabilityConfig(
        boolean tracingEnabled,
        boolean metricsEnabled,
        String logLevel
    ) {}
}