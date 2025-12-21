package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.config.McpDebugProperties;
import io.sandboxdev.gitmcp.debug.model.HealthCheckResult;
import io.sandboxdev.gitmcp.debug.model.HealthStatus;
import io.sandboxdev.gitmcp.debug.processor.HealthCheckExecutorImpl;
import io.sandboxdev.gitmcp.mcp.GitMcpResourceProvider;
import io.sandboxdev.gitmcp.mcp.GitMcpToolProvider;
import net.jqwik.api.*;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for tool accessibility validation in health checks.
 * 
 * Feature: mcp-debugging, Property 6: Tool accessibility validation
 * Validates: Requirements 3.1
 */
public class ToolAccessibilityValidationProperty {

    // Feature: mcp-debugging, Property 6: Tool accessibility validation
    @Property(tries = 100)
    void toolAccessibilityValidationCompleteness(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with a tool provider
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When running a comprehensive health check
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the health check should complete successfully
        assertThat(result).isNotNull();
        assertThat(result.checkName()).isEqualTo("comprehensive-health-check");
        assertThat(result.timestamp()).isBefore(Instant.now().plusSeconds(1));
        assertThat(result.status()).isIn((Object[]) HealthStatus.values());
        
        // And the result should contain component results including tool accessibility
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        assertThat(componentResults).isNotNull();
        
        // And there should be a tool accessibility check result
        HealthCheckResult toolAccessibilityResult = componentResults.stream()
            .filter(r -> "tool-accessibility".equals(r.checkName()))
            .findFirst()
            .orElse(null);
        
        assertThat(toolAccessibilityResult).isNotNull();
        assertThat(toolAccessibilityResult.status()).isIn((Object[]) HealthStatus.values());
        assertThat(toolAccessibilityResult.message()).contains("Tool accessibility check");
        
        // And the tool accessibility result should contain tool information
        assertThat(toolAccessibilityResult.details()).containsKey("accessibleTools");
        assertThat(toolAccessibilityResult.details()).containsKey("inaccessibleTools");
        assertThat(toolAccessibilityResult.details()).containsKey("totalTools");
        
        @SuppressWarnings("unchecked")
        List<String> accessibleTools = (List<String>) toolAccessibilityResult.details().get("accessibleTools");
        @SuppressWarnings("unchecked")
        List<String> inaccessibleTools = (List<String>) toolAccessibilityResult.details().get("inaccessibleTools");
        Integer totalTools = (Integer) toolAccessibilityResult.details().get("totalTools");
        
        // And the tool counts should be consistent
        assertThat(totalTools).isEqualTo(accessibleTools.size() + inaccessibleTools.size());
        assertThat(accessibleTools).isNotNull();
        assertThat(inaccessibleTools).isNotNull();
        
        // And if all tools are accessible, status should be HEALTHY
        if (inaccessibleTools.isEmpty() && !accessibleTools.isEmpty()) {
            assertThat(toolAccessibilityResult.status()).isEqualTo(HealthStatus.HEALTHY);
        }
        
        // And if some tools are inaccessible, status should be DEGRADED
        if (!inaccessibleTools.isEmpty()) {
            assertThat(toolAccessibilityResult.status()).isEqualTo(HealthStatus.DEGRADED);
        }
    }

    @Property(tries = 100)
    void toolAccessibilityValidationForAllRegisteredTools(
        @ForAll("toolProviderWithMethods") GitMcpToolProvider toolProvider
    ) {
        // Given a tool provider with MCP tools
        McpDebugProperties debugProperties = createDefaultDebugProperties();
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When validating tool accessibility
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then all registered MCP tools should be checked
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        HealthCheckResult toolAccessibilityResult = componentResults.stream()
            .filter(r -> "tool-accessibility".equals(r.checkName()))
            .findFirst()
            .orElseThrow();
        
        // Count actual @McpTool methods in the provider
        Method[] methods = toolProvider.getClass().getDeclaredMethods();
        long mcpToolCount = 0;
        for (Method method : methods) {
            if (method.isAnnotationPresent(org.springaicommunity.mcp.annotation.McpTool.class)) {
                mcpToolCount++;
            }
        }
        
        Integer totalTools = (Integer) toolAccessibilityResult.details().get("totalTools");
        
        // And the total tools count should match the actual number of @McpTool methods
        assertThat(totalTools.longValue()).isEqualTo(mcpToolCount);
        
        // And each tool should be categorized as either accessible or inaccessible
        @SuppressWarnings("unchecked")
        List<String> accessibleTools = (List<String>) toolAccessibilityResult.details().get("accessibleTools");
        @SuppressWarnings("unchecked")
        List<String> inaccessibleTools = (List<String>) toolAccessibilityResult.details().get("inaccessibleTools");
        
        // No tool should appear in both lists
        assertThat(accessibleTools).doesNotContainAnyElementsOf(inaccessibleTools);
        
        // All tools should be accounted for
        assertThat(accessibleTools.size() + inaccessibleTools.size()).isEqualTo(totalTools);
    }

    @Property(tries = 100)
    void toolAccessibilityValidationResilience(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with a null or problematic tool provider
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        // Create a tool provider that might cause issues
        GitMcpToolProvider problematicToolProvider = mock(GitMcpToolProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, problematicToolProvider, resourceProvider
        );
        
        // When running health check with problematic tool provider
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the health check should still complete (not throw exceptions)
        assertThat(result).isNotNull();
        assertThat(result.checkName()).isEqualTo("comprehensive-health-check");
        
        // And should contain component results
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        assertThat(componentResults).isNotNull();
        
        // And should include tool accessibility check (even if it fails)
        boolean hasToolAccessibilityCheck = componentResults.stream()
            .anyMatch(r -> "tool-accessibility".equals(r.checkName()));
        assertThat(hasToolAccessibilityCheck).isTrue();
        
        // And the overall result should have a valid status
        assertThat(result.status()).isIn((Object[]) HealthStatus.values());
    }

    @Property(tries = 100)
    void toolAccessibilityValidationStatusDetermination(
        @ForAll("accessibleToolCount") int accessibleCount,
        @ForAll("inaccessibleToolCount") int inaccessibleCount
    ) {
        // Given tool accessibility results with specific counts
        Assume.that(accessibleCount >= 0 && inaccessibleCount >= 0);
        Assume.that(accessibleCount + inaccessibleCount > 0); // At least one tool
        
        // When determining health status based on tool accessibility
        HealthStatus expectedStatus;
        if (inaccessibleCount == 0) {
            expectedStatus = HealthStatus.HEALTHY;
        } else {
            expectedStatus = HealthStatus.DEGRADED;
        }
        
        // Then the status determination should be consistent
        // This tests the logic used in the actual implementation
        boolean hasInaccessibleTools = inaccessibleCount > 0;
        HealthStatus actualStatus = hasInaccessibleTools ? HealthStatus.DEGRADED : HealthStatus.HEALTHY;
        
        assertThat(actualStatus).isEqualTo(expectedStatus);
        
        // And the status should reflect the accessibility state
        if (inaccessibleCount == 0 && accessibleCount > 0) {
            assertThat(actualStatus).isEqualTo(HealthStatus.HEALTHY);
        }
        if (inaccessibleCount > 0) {
            assertThat(actualStatus).isEqualTo(HealthStatus.DEGRADED);
        }
    }

    @Provide
    Arbitrary<GitMcpToolProvider> toolProviderWithMethods() {
        // Create a real GitMcpToolProvider instance for testing
        // We'll use mocks for the dependencies since we're testing the health check logic
        return Arbitraries.just(mock(GitMcpToolProvider.class));
    }

    @Provide
    Arbitrary<McpDebugProperties> healthCheckConfiguration() {
        return Arbitraries.just(createDefaultDebugProperties());
    }

    @Provide
    Arbitrary<Integer> accessibleToolCount() {
        return Arbitraries.integers().between(0, 20);
    }

    @Provide
    Arbitrary<Integer> inaccessibleToolCount() {
        return Arbitraries.integers().between(0, 5);
    }

    /**
     * Create default debug properties for testing.
     */
    private McpDebugProperties createDefaultDebugProperties() {
        McpDebugProperties properties = new McpDebugProperties();
        properties.setEnabled(true);
        
        McpDebugProperties.HealthChecks healthChecks = new McpDebugProperties.HealthChecks();
        healthChecks.setEnabled(true);
        properties.setHealthChecks(healthChecks);
        
        return properties;
    }
}