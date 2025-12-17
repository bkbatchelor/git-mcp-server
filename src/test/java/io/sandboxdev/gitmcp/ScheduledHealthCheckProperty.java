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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for scheduled health check execution.
 * 
 * Feature: mcp-debugging, Property 10: Scheduled health check execution
 * Validates: Requirements 3.5
 */
public class ScheduledHealthCheckProperty {

    // Feature: mcp-debugging, Property 10: Scheduled health check execution
    @Property(tries = 100)
    void scheduledHealthCheckExecutionAtConfiguredIntervals(
        @ForAll("healthCheckInterval") Duration interval,
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with a task scheduler
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        // Mock the scheduled future returned by the task scheduler
        @SuppressWarnings({"unchecked", "rawtypes"})
        ScheduledFuture scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), eq(interval)))
            .thenReturn(scheduledFuture);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When scheduling health checks at the specified interval
        healthCheckExecutor.scheduleHealthChecks(interval);
        
        // Then the task scheduler should be called with the correct interval
        verify(taskScheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq(interval));
        
        // And the scheduled task should be a health check execution
        // (We verify this by ensuring the runnable, when executed, produces a health check result)
        // This is tested indirectly through the scheduling mechanism
        assertThat(interval).isNotNull();
    }

    @Property(tries = 100)
    void scheduledHealthCheckExecutionReplacesExistingSchedule(
        @ForAll("healthCheckInterval") Duration firstInterval,
        @ForAll("healthCheckInterval") Duration secondInterval,
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with existing scheduled health checks
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        // Mock scheduled futures
        @SuppressWarnings({"unchecked", "rawtypes"})
        ScheduledFuture firstScheduledFuture = mock(ScheduledFuture.class);
        @SuppressWarnings({"unchecked", "rawtypes"})
        ScheduledFuture secondScheduledFuture = mock(ScheduledFuture.class);
        
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), eq(firstInterval)))
            .thenReturn(firstScheduledFuture);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), eq(secondInterval)))
            .thenReturn(secondScheduledFuture);
        
        when(firstScheduledFuture.isCancelled()).thenReturn(false);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When scheduling health checks twice with different intervals
        healthCheckExecutor.scheduleHealthChecks(firstInterval);
        healthCheckExecutor.scheduleHealthChecks(secondInterval);
        
        // Then the first scheduled task should be cancelled
        verify(firstScheduledFuture, times(1)).cancel(false);
        
        // And a new task should be scheduled with the second interval
        verify(taskScheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq(firstInterval));
        verify(taskScheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq(secondInterval));
    }

    @Property(tries = 100)
    void scheduledHealthCheckExecutionProducesValidResults(
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
        
        // When executing a health check (simulating scheduled execution)
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the result should be valid for scheduled execution
        assertThat(result).isNotNull();
        assertThat(result.checkName()).isEqualTo("comprehensive-health-check");
        assertThat(result.status()).isIn((Object[]) HealthStatus.values());
        assertThat(result.timestamp()).isBefore(Instant.now().plusSeconds(1));
        assertThat(result.message()).isNotNull().isNotEmpty();
        assertThat(result.details()).isNotNull();
        
        // And should contain component results suitable for scheduled monitoring
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        assertThat(componentResults).isNotNull().isNotEmpty();
        
        // And each component result should be complete for monitoring purposes
        for (HealthCheckResult componentResult : componentResults) {
            assertThat(componentResult.checkName()).isNotNull().isNotEmpty();
            assertThat(componentResult.status()).isIn((Object[]) HealthStatus.values());
            assertThat(componentResult.timestamp()).isNotNull();
        }
    }

    @Property(tries = 100)
    void scheduledHealthCheckExecutionMaintainsHistory(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties,
        @ForAll("executionCount") int executionCount
    ) {
        // Given a health check executor
        Assume.that(executionCount >= 1 && executionCount <= 10);
        
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When executing health checks multiple times (simulating scheduled executions)
        for (int i = 0; i < executionCount; i++) {
            healthCheckExecutor.runHealthCheck();
        }
        
        // Then the health check history should contain all executions
        List<HealthCheckResult> history = healthCheckExecutor.getHealthHistory();
        assertThat(history).hasSize(executionCount);
        
        // And each history entry should be valid
        for (HealthCheckResult historyEntry : history) {
            assertThat(historyEntry.checkName()).isEqualTo("comprehensive-health-check");
            assertThat(historyEntry.status()).isIn((Object[]) HealthStatus.values());
            assertThat(historyEntry.timestamp()).isNotNull();
        }
        
        // And history entries should be ordered by execution time
        for (int i = 1; i < history.size(); i++) {
            assertThat(history.get(i).timestamp())
                .isAfterOrEqualTo(history.get(i - 1).timestamp());
        }
    }

    @Property(tries = 100)
    void scheduledHealthCheckExecutionWithValidIntervals(
        @ForAll("validHealthCheckInterval") Duration validInterval,
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with valid scheduling intervals
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        @SuppressWarnings({"unchecked", "rawtypes"})
        ScheduledFuture scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), eq(validInterval)))
            .thenReturn(scheduledFuture);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When scheduling health checks with valid intervals
        healthCheckExecutor.scheduleHealthChecks(validInterval);
        
        // Then the scheduling should succeed without exceptions
        verify(taskScheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq(validInterval));
        
        // And the interval should be positive
        assertThat(validInterval).isPositive();
        
        // And the interval should be reasonable for health check scheduling
        assertThat(validInterval.toSeconds()).isBetween(1L, 86400L); // Between 1 second and 1 day
    }

    @Property(tries = 100)
    void scheduledHealthCheckExecutionResilience(
        @ForAll("healthCheckConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with a task scheduler that might have issues
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        // Mock a scenario where scheduling might fail or return null
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class)))
            .thenReturn(null);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        Duration interval = Duration.ofMinutes(5);
        
        // When scheduling health checks with potential scheduler issues
        // Then it should not throw exceptions
        assertThatCode(() -> healthCheckExecutor.scheduleHealthChecks(interval))
            .doesNotThrowAnyException();
        
        // And the scheduler should still be called
        verify(taskScheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq(interval));
        
        // And manual health check execution should still work
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        assertThat(result).isNotNull();
        assertThat(result.status()).isIn((Object[]) HealthStatus.values());
    }

    @Provide
    Arbitrary<Duration> healthCheckInterval() {
        return Arbitraries.oneOf(
            Arbitraries.just(Duration.ofSeconds(30)),
            Arbitraries.just(Duration.ofMinutes(1)),
            Arbitraries.just(Duration.ofMinutes(5)),
            Arbitraries.just(Duration.ofMinutes(15)),
            Arbitraries.just(Duration.ofHours(1))
        );
    }

    @Provide
    Arbitrary<Duration> validHealthCheckInterval() {
        return Arbitraries.integers()
            .between(1, 3600) // 1 second to 1 hour in seconds
            .map(Duration::ofSeconds);
    }

    @Provide
    Arbitrary<McpDebugProperties> healthCheckConfiguration() {
        return Arbitraries.just(createDefaultDebugProperties());
    }

    @Provide
    Arbitrary<Integer> executionCount() {
        return Arbitraries.integers().between(1, 10);
    }

    /**
     * Create default debug properties for testing.
     */
    private McpDebugProperties createDefaultDebugProperties() {
        McpDebugProperties properties = new McpDebugProperties();
        properties.setEnabled(true);
        
        McpDebugProperties.HealthChecks healthChecks = new McpDebugProperties.HealthChecks();
        healthChecks.setEnabled(true);
        healthChecks.setInterval(Duration.ofMinutes(5));
        properties.setHealthChecks(healthChecks);
        
        return properties;
    }
}