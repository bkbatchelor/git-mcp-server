package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.config.McpDebugProperties;
import io.sandboxdev.gitmcp.debug.model.HealthCheck;
import io.sandboxdev.gitmcp.debug.model.HealthCheckResult;
import io.sandboxdev.gitmcp.debug.model.HealthStatus;
import io.sandboxdev.gitmcp.debug.processor.HealthCheckExecutorImpl;
import io.sandboxdev.gitmcp.mcp.GitMcpResourceProvider;
import io.sandboxdev.gitmcp.mcp.GitMcpToolProvider;
import net.jqwik.api.*;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for health report completeness in health checks.
 * 
 * Feature: mcp-debugging, Property 9: Health report completeness
 * Validates: Requirements 3.4
 */
public class HealthReportCompletenessProperty {

    // Feature: mcp-debugging, Property 9: Health report completeness
    @Property(tries = 100)
    void healthReportCompletenessForAllComponents(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties,
        @ForAll("customHealthCheckCount") int customHealthCheckCount
    ) {
        // Given a health check executor with custom health checks
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // Add custom health checks
        for (int i = 0; i < customHealthCheckCount; i++) {
            HealthCheck customCheck = createMockHealthCheck("custom-check-" + i, HealthStatus.HEALTHY);
            healthCheckExecutor.addCustomHealthCheck(customCheck);
        }
        
        // When running a comprehensive health check
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the health report should be complete with pass/fail indicators
        assertThat(result).isNotNull();
        assertThat(result.checkName()).isEqualTo("comprehensive-health-check");
        assertThat(result.status()).isIn((Object[]) HealthStatus.values());
        assertThat(result.message()).isNotNull().isNotEmpty();
        assertThat(result.timestamp()).isBefore(Instant.now().plusSeconds(1));
        assertThat(result.details()).isNotNull();
        
        // And the result should contain component results for all tested components
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        assertThat(componentResults).isNotNull();
        
        // And should include all core health checks
        boolean hasToolAccessibilityCheck = componentResults.stream()
            .anyMatch(r -> "tool-accessibility".equals(r.checkName()));
        assertThat(hasToolAccessibilityCheck).isTrue();
        
        boolean hasResourceEndpointCheck = componentResults.stream()
            .anyMatch(r -> "resource-endpoints".equals(r.checkName()));
        assertThat(hasResourceEndpointCheck).isTrue();
        
        boolean hasConfigurationCheck = componentResults.stream()
            .anyMatch(r -> "configuration-validation".equals(r.checkName()));
        assertThat(hasConfigurationCheck).isTrue();
        
        // And should include all custom health checks
        for (int i = 0; i < customHealthCheckCount; i++) {
            String customCheckName = "custom-check-" + i;
            boolean hasCustomCheck = componentResults.stream()
                .anyMatch(r -> customCheckName.equals(r.checkName()));
            assertThat(hasCustomCheck).isTrue();
        }
        
        // And each component result should have pass/fail indicators
        for (HealthCheckResult componentResult : componentResults) {
            assertThat(componentResult.checkName()).isNotNull().isNotEmpty();
            assertThat(componentResult.status()).isIn((Object[]) HealthStatus.values());
            assertThat(componentResult.message()).isNotNull();
            assertThat(componentResult.timestamp()).isNotNull();
            assertThat(componentResult.details()).isNotNull();
        }
        
        // And the total component count should match expected
        int expectedComponentCount = 3 + customHealthCheckCount; // 3 core checks + custom checks
        assertThat(componentResults).hasSize(expectedComponentCount);
    }

    @Property(tries = 100)
    void healthReportCompletenessWithExecutionMetadata(
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
        
        // When running a health check
        Instant beforeExecution = Instant.now();
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        Instant afterExecution = Instant.now();
        
        // Then the health report should contain complete execution metadata
        assertThat(result.timestamp()).isBetween(beforeExecution, afterExecution);
        
        // And should contain execution time information
        assertThat(result.details()).containsKey("executionTimeMs");
        Long executionTimeMs = (Long) result.details().get("executionTimeMs");
        assertThat(executionTimeMs).isNotNull().isGreaterThanOrEqualTo(0L);
        
        // And should contain component count information
        assertThat(result.message()).contains("components checked");
        
        // And should contain component results
        assertThat(result.details()).containsKey("componentResults");
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        assertThat(componentResults).isNotNull().isNotEmpty();
        
        // And the execution time should be reasonable (not negative or extremely large)
        assertThat(executionTimeMs).isLessThan(30000L); // Should complete within 30 seconds
    }

    @Property(tries = 100)
    void healthReportCompletenessWithVariousComponentStatuses(
        @ForAll("healthyCheckCount") int healthyCount,
        @ForAll("degradedCheckCount") int degradedCount,
        @ForAll("unhealthyCheckCount") int unhealthyCount
    ) {
        // Given a health check executor with custom checks having various statuses
        Assume.that(healthyCount >= 0 && degradedCount >= 0 && unhealthyCount >= 0);
        Assume.that(healthyCount + degradedCount + unhealthyCount > 0);
        
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        McpDebugProperties debugProperties = createDefaultDebugProperties();
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // Add custom health checks with different statuses
        for (int i = 0; i < healthyCount; i++) {
            HealthCheck healthyCheck = createMockHealthCheck("healthy-" + i, HealthStatus.HEALTHY);
            healthCheckExecutor.addCustomHealthCheck(healthyCheck);
        }
        
        for (int i = 0; i < degradedCount; i++) {
            HealthCheck degradedCheck = createMockHealthCheck("degraded-" + i, HealthStatus.DEGRADED);
            healthCheckExecutor.addCustomHealthCheck(degradedCheck);
        }
        
        for (int i = 0; i < unhealthyCount; i++) {
            HealthCheck unhealthyCheck = createMockHealthCheck("unhealthy-" + i, HealthStatus.UNHEALTHY);
            healthCheckExecutor.addCustomHealthCheck(unhealthyCheck);
        }
        
        // When running a health check
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the health report should reflect the overall status correctly
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        
        // Count actual statuses in component results
        long actualHealthyCount = componentResults.stream()
            .filter(r -> r.status() == HealthStatus.HEALTHY)
            .count();
        long actualDegradedCount = componentResults.stream()
            .filter(r -> r.status() == HealthStatus.DEGRADED)
            .count();
        long actualUnhealthyCount = componentResults.stream()
            .filter(r -> r.status() == HealthStatus.UNHEALTHY)
            .count();
        
        // And the overall status should be determined correctly
        if (unhealthyCount > 0 || actualUnhealthyCount > 0) {
            assertThat(result.status()).isEqualTo(HealthStatus.UNHEALTHY);
        } else if (degradedCount > 0 || actualDegradedCount > 0) {
            assertThat(result.status()).isEqualTo(HealthStatus.DEGRADED);
        } else if (actualHealthyCount > 0) {
            assertThat(result.status()).isEqualTo(HealthStatus.HEALTHY);
        }
        
        // And all component results should have valid pass/fail indicators
        for (HealthCheckResult componentResult : componentResults) {
            assertThat(componentResult.status()).isIn((Object[]) HealthStatus.values());
            assertThat(componentResult.checkName()).isNotNull().isNotEmpty();
        }
    }

    @Property(tries = 100)
    void healthReportCompletenessWithFailedCustomChecks(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with a custom check that throws exceptions
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // Add a custom health check that throws an exception
        HealthCheck failingCheck = mock(HealthCheck.class);
        when(failingCheck.getName()).thenReturn("failing-check");
        when(failingCheck.execute()).thenThrow(new RuntimeException("Test exception"));
        healthCheckExecutor.addCustomHealthCheck(failingCheck);
        
        // When running a health check
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the health report should still be complete despite the failure
        assertThat(result).isNotNull();
        assertThat(result.status()).isIn((Object[]) HealthStatus.values());
        
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        assertThat(componentResults).isNotNull();
        
        // And should include the failed custom check with appropriate status
        HealthCheckResult failingCheckResult = componentResults.stream()
            .filter(r -> "failing-check".equals(r.checkName()))
            .findFirst()
            .orElse(null);
        
        assertThat(failingCheckResult).isNotNull();
        assertThat(failingCheckResult.status()).isEqualTo(HealthStatus.UNHEALTHY);
        assertThat(failingCheckResult.message()).contains("execution failed");
        assertThat(failingCheckResult.details()).containsKey("error");
        
        // And the overall status should reflect the failure
        assertThat(result.status()).isEqualTo(HealthStatus.UNHEALTHY);
    }

    @Property(tries = 100)
    void healthReportCompletenessStructure(
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
        
        // When running a health check
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the health report should have the correct structure
        assertThat(result.checkName()).isEqualTo("comprehensive-health-check");
        assertThat(result.status()).isNotNull();
        assertThat(result.message()).isNotNull().isNotEmpty();
        assertThat(result.timestamp()).isNotNull();
        assertThat(result.details()).isNotNull();
        
        // And should contain all required details
        Map<String, Object> details = result.details();
        assertThat(details).containsKey("componentResults");
        assertThat(details).containsKey("executionTimeMs");
        
        // And component results should be properly structured
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) details.get("componentResults");
        assertThat(componentResults).isNotNull();
        
        for (HealthCheckResult componentResult : componentResults) {
            // Each component result should be complete
            assertThat(componentResult.checkName()).isNotNull().isNotEmpty();
            assertThat(componentResult.status()).isNotNull();
            assertThat(componentResult.message()).isNotNull();
            assertThat(componentResult.timestamp()).isNotNull();
            assertThat(componentResult.details()).isNotNull();
        }
    }

    @Provide
    Arbitrary<McpDebugProperties> healthCheckConfiguration() {
        return Arbitraries.just(createDefaultDebugProperties());
    }

    @Provide
    Arbitrary<Integer> customHealthCheckCount() {
        return Arbitraries.integers().between(0, 5);
    }

    @Provide
    Arbitrary<Integer> healthyCheckCount() {
        return Arbitraries.integers().between(0, 3);
    }

    @Provide
    Arbitrary<Integer> degradedCheckCount() {
        return Arbitraries.integers().between(0, 3);
    }

    @Provide
    Arbitrary<Integer> unhealthyCheckCount() {
        return Arbitraries.integers().between(0, 3);
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

    /**
     * Create a mock health check with the specified status.
     */
    private HealthCheck createMockHealthCheck(String name, HealthStatus status) {
        HealthCheck check = mock(HealthCheck.class);
        when(check.getName()).thenReturn(name);
        when(check.execute()).thenReturn(new HealthCheckResult(
            name,
            status,
            "Mock health check result",
            Instant.now(),
            Map.of("mock", true)
        ));
        return check;
    }
}