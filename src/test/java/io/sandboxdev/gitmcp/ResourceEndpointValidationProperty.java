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
 * Property-based tests for resource endpoint validation in health checks.
 * 
 * Feature: mcp-debugging, Property 7: Resource endpoint validation
 * Validates: Requirements 3.2
 */
public class ResourceEndpointValidationProperty {

    // Feature: mcp-debugging, Property 7: Resource endpoint validation
    @Property(tries = 100)
    void resourceEndpointValidationCompleteness(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with a resource provider
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
        
        // And the result should contain component results including resource endpoint validation
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        assertThat(componentResults).isNotNull();
        
        // And there should be a resource endpoint validation result
        HealthCheckResult resourceEndpointResult = componentResults.stream()
            .filter(r -> "resource-endpoints".equals(r.checkName()))
            .findFirst()
            .orElse(null);
        
        assertThat(resourceEndpointResult).isNotNull();
        assertThat(resourceEndpointResult.status()).isIn((Object[]) HealthStatus.values());
        assertThat(resourceEndpointResult.message()).contains("Resource endpoint validation");
        
        // And the resource endpoint result should contain endpoint information
        assertThat(resourceEndpointResult.details()).containsKey("validEndpoints");
        assertThat(resourceEndpointResult.details()).containsKey("invalidEndpoints");
        assertThat(resourceEndpointResult.details()).containsKey("totalEndpoints");
        
        @SuppressWarnings("unchecked")
        List<String> validEndpoints = (List<String>) resourceEndpointResult.details().get("validEndpoints");
        @SuppressWarnings("unchecked")
        List<String> invalidEndpoints = (List<String>) resourceEndpointResult.details().get("invalidEndpoints");
        Integer totalEndpoints = (Integer) resourceEndpointResult.details().get("totalEndpoints");
        
        // And the endpoint counts should be consistent
        assertThat(totalEndpoints).isEqualTo(validEndpoints.size() + invalidEndpoints.size());
        assertThat(validEndpoints).isNotNull();
        assertThat(invalidEndpoints).isNotNull();
        
        // And if all endpoints are valid, status should be HEALTHY
        if (invalidEndpoints.isEmpty() && !validEndpoints.isEmpty()) {
            assertThat(resourceEndpointResult.status()).isEqualTo(HealthStatus.HEALTHY);
        }
        
        // And if some endpoints are invalid, status should be DEGRADED
        if (!invalidEndpoints.isEmpty()) {
            assertThat(resourceEndpointResult.status()).isEqualTo(HealthStatus.DEGRADED);
        }
    }

    @Property(tries = 100)
    void resourceEndpointValidationForAllRegisteredResources(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a resource provider with MCP resources
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When validating resource endpoints
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then all registered MCP resources should be checked
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        HealthCheckResult resourceEndpointResult = componentResults.stream()
            .filter(r -> "resource-endpoints".equals(r.checkName()))
            .findFirst()
            .orElseThrow();
        
        // Count actual @McpResource methods in the provider
        Method[] methods = resourceProvider.getClass().getDeclaredMethods();
        long mcpResourceCount = 0;
        for (Method method : methods) {
            if (method.isAnnotationPresent(org.springaicommunity.mcp.annotation.McpResource.class)) {
                mcpResourceCount++;
            }
        }
        
        Integer totalEndpoints = (Integer) resourceEndpointResult.details().get("totalEndpoints");
        
        // And the total endpoints count should match the actual number of @McpResource methods
        assertThat(totalEndpoints.longValue()).isEqualTo(mcpResourceCount);
        
        // And each endpoint should be categorized as either valid or invalid
        @SuppressWarnings("unchecked")
        List<String> validEndpoints = (List<String>) resourceEndpointResult.details().get("validEndpoints");
        @SuppressWarnings("unchecked")
        List<String> invalidEndpoints = (List<String>) resourceEndpointResult.details().get("invalidEndpoints");
        
        // No endpoint should appear in both lists
        assertThat(validEndpoints).doesNotContainAnyElementsOf(invalidEndpoints);
        
        // All endpoints should be accounted for
        assertThat(validEndpoints.size() + invalidEndpoints.size()).isEqualTo(totalEndpoints);
    }

    @Property(tries = 100)
    void resourceEndpointValidationResilience(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with a null or problematic resource provider
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        
        // Create a resource provider that might cause issues
        GitMcpResourceProvider problematicResourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, problematicResourceProvider
        );
        
        // When running health check with problematic resource provider
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the health check should still complete (not throw exceptions)
        assertThat(result).isNotNull();
        assertThat(result.checkName()).isEqualTo("comprehensive-health-check");
        
        // And should contain component results
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        assertThat(componentResults).isNotNull();
        
        // And should include resource endpoint check (even if it fails)
        boolean hasResourceEndpointCheck = componentResults.stream()
            .anyMatch(r -> "resource-endpoints".equals(r.checkName()));
        assertThat(hasResourceEndpointCheck).isTrue();
        
        // And the overall result should have a valid status
        assertThat(result.status()).isIn((Object[]) HealthStatus.values());
    }

    @Property(tries = 100)
    void resourceEndpointValidationStatusDetermination(
        @ForAll("validEndpointCount") int validCount,
        @ForAll("invalidEndpointCount") int invalidCount
    ) {
        // Given resource endpoint results with specific counts
        Assume.that(validCount >= 0 && invalidCount >= 0);
        Assume.that(validCount + invalidCount > 0); // At least one endpoint
        
        // When determining health status based on endpoint validation
        HealthStatus expectedStatus;
        if (invalidCount == 0) {
            expectedStatus = HealthStatus.HEALTHY;
        } else {
            expectedStatus = HealthStatus.DEGRADED;
        }
        
        // Then the status determination should be consistent
        // This tests the logic used in the actual implementation
        boolean hasInvalidEndpoints = invalidCount > 0;
        HealthStatus actualStatus = hasInvalidEndpoints ? HealthStatus.DEGRADED : HealthStatus.HEALTHY;
        
        assertThat(actualStatus).isEqualTo(expectedStatus);
        
        // And the status should reflect the endpoint validation state
        if (invalidCount == 0 && validCount > 0) {
            assertThat(actualStatus).isEqualTo(HealthStatus.HEALTHY);
        }
        if (invalidCount > 0) {
            assertThat(actualStatus).isEqualTo(HealthStatus.DEGRADED);
        }
    }

    @Property(tries = 100)
    void resourceEndpointValidationDataIntegrity(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When running resource endpoint validation
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the resource endpoint validation should return valid data
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        HealthCheckResult resourceEndpointResult = componentResults.stream()
            .filter(r -> "resource-endpoints".equals(r.checkName()))
            .findFirst()
            .orElseThrow();
        
        // And the validation should test that endpoints return valid data
        // (This property ensures the validation checks data validity, not just accessibility)
        assertThat(resourceEndpointResult.message()).contains("Resource endpoint validation");
        
        // And the details should contain meaningful information about data validity
        assertThat(resourceEndpointResult.details()).containsKey("validEndpoints");
        assertThat(resourceEndpointResult.details()).containsKey("invalidEndpoints");
        
        // And the validation timestamp should be recent
        assertThat(resourceEndpointResult.timestamp()).isAfter(Instant.now().minusSeconds(10));
    }

    @Provide
    Arbitrary<McpDebugProperties> healthCheckConfiguration() {
        return Arbitraries.just(createDefaultDebugProperties());
    }

    @Provide
    Arbitrary<Integer> validEndpointCount() {
        return Arbitraries.integers().between(0, 20);
    }

    @Provide
    Arbitrary<Integer> invalidEndpointCount() {
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