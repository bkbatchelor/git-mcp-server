package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Main Spring Boot application class for the Git MCP Server.
 * 
 * This application implements the Model Context Protocol (MCP) JSON-RPC 2.0 specification
 * to provide secure Git operations to Large Language Models (LLMs).
 * 
 * Key features:
 * - Java 21 with Virtual Threads for high-throughput I/O
 * - Dual transport support: Stdio and Server-Sent Events (SSE)
 * - JGit pure Java implementation for Git operations
 * - Comprehensive input validation and security guardrails
 * - Distributed tracing and metrics via Micrometer
 * - Stateless operation for horizontal scalability
 */
@SpringBootApplication
@EnableConfigurationProperties(GitMcpProperties.class)
public class GitMcpServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(GitMcpServerApplication.class);

    public static void main(String[] args) {
        // Enable Virtual Threads for Spring Boot
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(GitMcpServerApplication.class, args);
    }

    /**
     * Startup validation and health checks.
     * Requirement 1.4: Application startup validation
     * Requirement 15.1: Configuration validation
     */
    @Bean
    public CommandLineRunner startupValidator(GitMcpProperties properties) {
        return args -> {
            logger.info("Starting Git MCP Server with configuration validation...");
            
            // Validate transport configuration
            validateTransportConfiguration(properties.transport());
            
            // Validate security configuration
            validateSecurityConfiguration(properties.security());
            
            // Log startup completion
            logger.info("Git MCP Server started successfully");
            logger.info("Transport: Stdio={}, SSE={}", 
                properties.transport().stdioEnabled(), 
                properties.transport().sseEnabled());
            logger.info("Security: Input sanitization={}, Allowed repositories={}", 
                properties.security().enableInputSanitization(),
                properties.security().allowedRepositories().size());
        };
    }

    private void validateTransportConfiguration(GitMcpProperties.TransportConfig transport) {
        if (!transport.stdioEnabled() && !transport.sseEnabled()) {
            throw new IllegalStateException("At least one transport (Stdio or SSE) must be enabled");
        }
        logger.debug("Transport configuration validated successfully");
    }

    private void validateSecurityConfiguration(GitMcpProperties.SecurityConfig security) {
        if (security.allowedRepositories().isEmpty()) {
            throw new IllegalStateException("At least one repository must be allowed in security configuration");
        }
        logger.debug("Security configuration validated successfully");
    }
}