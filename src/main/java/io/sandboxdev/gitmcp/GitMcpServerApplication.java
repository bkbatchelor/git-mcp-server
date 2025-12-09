package io.sandboxdev.gitmcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Main Spring Boot application class for Git MCP Server.
 * Provides Git operations through the Model Context Protocol.
 */
@SpringBootApplication
public class GitMcpServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(GitMcpServerApplication.class);
    
    private final Environment environment;

    public GitMcpServerApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        logger.info("Starting Git MCP Server...");
        SpringApplication.run(GitMcpServerApplication.class, args);
    }

    /**
     * Log startup information when application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String serverName = environment.getProperty("git-mcp-server.mcp.server-name", "Git MCP Server");
        String serverVersion = environment.getProperty("git-mcp-server.mcp.server-version", "1.0.0");
        String maxCachedRepos = environment.getProperty("git-mcp-server.repository.cache.max-size", "10");
        String cacheTimeout = environment.getProperty("git-mcp-server.repository.cache.timeout", "30m");
        String maxHistoryLimit = environment.getProperty("git-mcp-server.history.max-limit", "1000");
        String authEnabled = environment.getProperty("git-mcp-server.authentication.enabled", "true");
        
        logger.info("=".repeat(60));
        logger.info("Git MCP Server Started Successfully");
        logger.info("=".repeat(60));
        logger.info("Server Name: {}", serverName);
        logger.info("Server Version: {}", serverVersion);
        logger.info("Configuration:");
        logger.info("  - Max Cached Repositories: {}", maxCachedRepos);
        logger.info("  - Cache Timeout: {}", cacheTimeout);
        logger.info("  - Max History Limit: {}", maxHistoryLimit);
        logger.info("  - Authentication Enabled: {}", authEnabled);
        logger.info("=".repeat(60));
        logger.info("Git MCP Server is ready to accept connections");
        logger.info("=".repeat(60));
    }
}
