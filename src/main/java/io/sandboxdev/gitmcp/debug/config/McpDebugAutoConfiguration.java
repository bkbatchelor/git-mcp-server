package io.sandboxdev.gitmcp.debug.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for the MCP Debug System.
 * Enables AOP and configures debug components when debug is enabled.
 */
@Configuration
@ConditionalOnProperty(name = "mcp-debug.enabled", havingValue = "true", matchIfMissing = false)
@EnableAspectJAutoProxy
@EnableConfigurationProperties(McpDebugProperties.class)
public class McpDebugAutoConfiguration {
    
    // Bean configurations will be added in subsequent tasks
}