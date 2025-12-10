package io.sandboxdev.gitmcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Main Spring Boot application class for the Git MCP Server.
 * 
 * <p>This class serves as the entry point for the Git MCP Server application,
 * which provides Git repository operations through the Model Context Protocol (MCP).
 * The server exposes Git functionality as MCP tools and resources that can be
 * consumed by AI assistants and other MCP-compatible clients.</p>
 * 
 * <p>The application is built using Spring Boot and integrates with Spring AI's
 * MCP framework to provide standardized protocol support. It uses JGit for
 * Git operations and includes comprehensive error handling and logging.</p>
 * 
 * <p>Key features provided by this application:</p>
 * <ul>
 *   <li>Repository management (init, clone, status)</li>
 *   <li>Commit operations (stage, commit, history)</li>
 *   <li>Branch management (create, switch, delete)</li>
 *   <li>Remote operations (push, pull, fetch)</li>
 *   <li>Commit message generation</li>
 *   <li>File content and diff retrieval</li>
 * </ul>
 * 
 * @author Git MCP Server
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties
public class GitMcpServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(GitMcpServerApplication.class);
    
    private final Environment environment;

    /**
     * Constructs the Git MCP Server application with the given Spring environment.
     * 
     * @param environment the Spring environment for accessing configuration properties
     */
    public GitMcpServerApplication(Environment environment) {
        this.environment = environment;
    }

    /**
     * Main method to start the Git MCP Server application.
     * 
     * <p>Initializes the Spring Boot application context and starts the MCP server.
     * The server will be ready to accept MCP connections once startup is complete.</p>
     * 
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        logger.info("Starting Git MCP Server...");
        SpringApplication.run(GitMcpServerApplication.class, args);
    }

    /**
     * Provides an ObjectMapper bean for JSON serialization and deserialization.
     * 
     * <p>This bean is used throughout the application for converting Java objects
     * to JSON and vice versa, particularly for MCP protocol communication.</p>
     * 
     * @return a configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Event listener that logs startup information when the application is ready.
     * 
     * <p>This method is called after the Spring Boot application has fully started
     * and is ready to accept requests. It logs configuration details and startup
     * status to help with monitoring and debugging.</p>
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
