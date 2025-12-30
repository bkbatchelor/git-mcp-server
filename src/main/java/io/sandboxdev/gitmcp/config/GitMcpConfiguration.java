package io.sandboxdev.gitmcp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to enable GitMcpProperties.
 * Requirement 15.1: Use @ConfigurationProperties for type-safe configuration
 * Requirement 15.4: Validate configuration at startup and fail fast if required properties missing
 */
@Configuration
@EnableConfigurationProperties(GitMcpProperties.class)
public class GitMcpConfiguration {
}
