package io.sandboxdev.gitmcp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Main configuration class for the Git MCP Server.
 * 
 * This configuration class enables the GitMcpProperties and sets up
 * the basic Spring Boot configuration for the MCP server.
 */
@Configuration
@EnableConfigurationProperties(GitMcpProperties.class)
public class GitMcpConfiguration {
    
    // Additional configuration beans will be added in subsequent tasks
}