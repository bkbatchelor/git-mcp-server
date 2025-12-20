package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.config.McpDebugProperties;
import io.sandboxdev.gitmcp.debug.model.HealthCheckResult;
import io.sandboxdev.gitmcp.debug.processor.HealthCheckExecutorImpl;
import io.sandboxdev.gitmcp.mcp.GitMcpResourceProvider;
import io.sandboxdev.gitmcp.mcp.GitMcpToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthCheckExecutor implementation.
 */
public class HealthCheckExecutorTest {
    
    private HealthCheckExecutorImpl healthCheckExecutor;
    private ApplicationContext applicationContext;
    private McpDebugProperties debugProperties;
    private TaskScheduler taskScheduler;
    private GitMcpToolProvider toolProvider;
    private GitMcpResourceProvider resourceProvider;
    
    @BeforeEach
    void setUp() {
        applicationContext = mock(ApplicationContext.class);
        debugProperties = createDefaultDebugProperties();
        taskScheduler = mock(TaskScheduler.class);
        toolProvider = mock(GitMcpToolProvider.class);
        resourceProvider = mock(GitMcpResourceProvider.class);
        
        healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
    }
    
    @Test
    void runHealthCheck_ShouldReturnValidResult() {
        // When running a health check
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the result should be valid
        assertThat(result).isNotNull();
        assertThat(result.checkName()).isEqualTo("comprehensive-health-check");
        assertThat(result.status()).isNotNull();
        assertThat(result.message()).isNotNull();
        assertThat(result.timestamp()).isNotNull();
        assertThat(result.details()).isNotNull();
    }
    
    @Test
    void runHealthCheck_ShouldIncludeComponentResults() {
        // When running a health check
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the result should include component results
        assertThat(result.details()).containsKey("componentResults");
        
        @SuppressWarnings("unchecked")
        java.util.List<HealthCheckResult> componentResults = 
            (java.util.List<HealthCheckResult>) result.details().get("componentResults");
        
        assertThat(componentResults).isNotNull();
        assertThat(componentResults).isNotEmpty();
        
        // Should include tool accessibility check
        boolean hasToolAccessibilityCheck = componentResults.stream()
            .anyMatch(r -> "tool-accessibility".equals(r.checkName()));
        assertThat(hasToolAccessibilityCheck).isTrue();
        
        // Should include resource endpoint check
        boolean hasResourceEndpointCheck = componentResults.stream()
            .anyMatch(r -> "resource-endpoints".equals(r.checkName()));
        assertThat(hasResourceEndpointCheck).isTrue();
        
        // Should include configuration validation check
        boolean hasConfigurationCheck = componentResults.stream()
            .anyMatch(r -> "configuration-validation".equals(r.checkName()));
        assertThat(hasConfigurationCheck).isTrue();
    }
    
    private McpDebugProperties createDefaultDebugProperties() {
        McpDebugProperties properties = new McpDebugProperties();
        properties.setEnabled(true);
        
        McpDebugProperties.HealthChecks healthChecks = new McpDebugProperties.HealthChecks();
        healthChecks.setEnabled(true);
        properties.setHealthChecks(healthChecks);
        
        return properties;
    }
}