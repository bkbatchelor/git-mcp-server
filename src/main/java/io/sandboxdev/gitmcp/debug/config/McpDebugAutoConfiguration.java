package io.sandboxdev.gitmcp.debug.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Auto-configuration for the MCP Debug System.
 * Enables AOP and configures debug components when debug is enabled.
 */
@Configuration
@ConditionalOnProperty(name = "mcp-debug.enabled", havingValue = "true", matchIfMissing = false)
@EnableAspectJAutoProxy
@EnableConfigurationProperties(McpDebugProperties.class)
public class McpDebugAutoConfiguration {
    
    /**
     * Configures a TaskScheduler for health check scheduling if none exists.
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskScheduler mcpDebugTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("mcp-debug-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}