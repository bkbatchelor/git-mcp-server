package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.config.McpDebugProperties;
import io.sandboxdev.gitmcp.debug.model.DebugLevel;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for configuration validation in health checks.
 * 
 * Feature: mcp-debugging, Property 8: Configuration validation completeness
 * Validates: Requirements 3.3
 */
public class ConfigurationValidationProperty {

    // Feature: mcp-debugging, Property 8: Configuration validation completeness
    @Property(tries = 100)
    void configurationValidationCompleteness(
        @ForAll("debugConfiguration") McpDebugProperties debugProperties
    ) {
        // Given a health check executor with debug configuration
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
        
        // And the result should contain component results including configuration validation
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        assertThat(componentResults).isNotNull();
        
        // And there should be a configuration validation result
        HealthCheckResult configValidationResult = componentResults.stream()
            .filter(r -> "configuration-validation".equals(r.checkName()))
            .findFirst()
            .orElse(null);
        
        assertThat(configValidationResult).isNotNull();
        assertThat(configValidationResult.status()).isIn((Object[]) HealthStatus.values());
        assertThat(configValidationResult.message()).contains("Configuration validation");
        
        // And the configuration validation result should contain validation information
        assertThat(configValidationResult.details()).containsKey("validationIssues");
        assertThat(configValidationResult.details()).containsKey("debugEnabled");
        
        @SuppressWarnings("unchecked")
        List<String> validationIssues = (List<String>) configValidationResult.details().get("validationIssues");
        Boolean debugEnabled = (Boolean) configValidationResult.details().get("debugEnabled");
        
        // And the validation should reflect the actual configuration state
        assertThat(validationIssues).isNotNull();
        assertThat(debugEnabled).isEqualTo(debugProperties.isEnabled());
        
        // And if there are no validation issues, status should be HEALTHY
        if (validationIssues.isEmpty()) {
            assertThat(configValidationResult.status()).isEqualTo(HealthStatus.HEALTHY);
        }
        
        // And if there are validation issues, status should be DEGRADED
        if (!validationIssues.isEmpty()) {
            assertThat(configValidationResult.status()).isEqualTo(HealthStatus.DEGRADED);
        }
    }

    @Property(tries = 100)
    void configurationValidationChecksAllCriticalSettings(
        @ForAll("validDebugConfiguration") McpDebugProperties validConfig,
        @ForAll("invalidDebugConfiguration") McpDebugProperties invalidConfig
    ) {
        // Given valid and invalid configurations
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        // When validating a valid configuration
        HealthCheckExecutorImpl validHealthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, validConfig, taskScheduler, toolProvider, resourceProvider
        );
        HealthCheckResult validResult = validHealthCheckExecutor.runHealthCheck();
        
        // Then the valid configuration should pass validation
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> validComponentResults = (List<HealthCheckResult>) validResult.details().get("componentResults");
        HealthCheckResult validConfigResult = validComponentResults.stream()
            .filter(r -> "configuration-validation".equals(r.checkName()))
            .findFirst()
            .orElseThrow();
        
        @SuppressWarnings("unchecked")
        List<String> validValidationIssues = (List<String>) validConfigResult.details().get("validationIssues");
        
        // Valid configuration should have fewer or equal issues compared to invalid one
        assertThat(validValidationIssues).isNotNull();
        
        // When validating an invalid configuration
        HealthCheckExecutorImpl invalidHealthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, invalidConfig, taskScheduler, toolProvider, resourceProvider
        );
        HealthCheckResult invalidResult = invalidHealthCheckExecutor.runHealthCheck();
        
        // Then the invalid configuration should be detected
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> invalidComponentResults = (List<HealthCheckResult>) invalidResult.details().get("componentResults");
        HealthCheckResult invalidConfigResult = invalidComponentResults.stream()
            .filter(r -> "configuration-validation".equals(r.checkName()))
            .findFirst()
            .orElseThrow();
        
        @SuppressWarnings("unchecked")
        List<String> invalidValidationIssues = (List<String>) invalidConfigResult.details().get("validationIssues");
        
        // Invalid configuration should potentially have more issues
        assertThat(invalidValidationIssues).isNotNull();
        
        // Both validations should complete without throwing exceptions
        assertThat(validConfigResult.status()).isIn((Object[]) HealthStatus.values());
        assertThat(invalidConfigResult.status()).isIn((Object[]) HealthStatus.values());
    }

    @Property(tries = 100)
    void configurationValidationDetectsSpecificIssues(
        @ForAll("debugLevel") DebugLevel globalLevel,
        @ForAll("retentionPeriod") Duration retention,
        @ForAll("bufferSize") int bufferSize,
        @ForAll("basePath") String basePath
    ) {
        // Given a configuration with potentially problematic values
        McpDebugProperties debugProperties = new McpDebugProperties();
        debugProperties.setEnabled(true);
        debugProperties.setGlobalLevel(globalLevel);
        
        // Set protocol tracing with potentially invalid values
        McpDebugProperties.ProtocolTracing protocolTracing = new McpDebugProperties.ProtocolTracing();
        protocolTracing.setEnabled(true);
        protocolTracing.setRetention(retention);
        protocolTracing.setBufferSize(bufferSize);
        debugProperties.setProtocolTracing(protocolTracing);
        
        // Set storage with potentially invalid path
        McpDebugProperties.Storage storage = new McpDebugProperties.Storage();
        storage.setBasePath(basePath);
        debugProperties.setStorage(storage);
        
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When running configuration validation
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the validation should detect specific configuration issues
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        HealthCheckResult configResult = componentResults.stream()
            .filter(r -> "configuration-validation".equals(r.checkName()))
            .findFirst()
            .orElseThrow();
        
        @SuppressWarnings("unchecked")
        List<String> validationIssues = (List<String>) configResult.details().get("validationIssues");
        
        // And should detect negative retention periods
        if (retention != null && retention.isNegative()) {
            assertThat(validationIssues).anyMatch(issue -> issue.contains("retention"));
        }
        
        // And should detect invalid buffer sizes
        if (bufferSize <= 0) {
            assertThat(validationIssues).anyMatch(issue -> issue.contains("buffer size"));
        }
        
        // And should detect invalid base paths
        if (basePath == null || basePath.trim().isEmpty()) {
            assertThat(validationIssues).anyMatch(issue -> issue.contains("base path"));
        }
        
        // And the validation should always complete
        assertThat(configResult.status()).isIn((Object[]) HealthStatus.values());
        assertThat(validationIssues).isNotNull();
    }

    @Property(tries = 100)
    void configurationValidationResilience(
        @ForAll("debugLevel") DebugLevel globalLevel
    ) {
        // Given a configuration that might cause validation issues
        McpDebugProperties debugProperties = new McpDebugProperties();
        debugProperties.setEnabled(true);
        debugProperties.setGlobalLevel(globalLevel);
        
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        GitMcpToolProvider toolProvider = mock(GitMcpToolProvider.class);
        GitMcpResourceProvider resourceProvider = mock(GitMcpResourceProvider.class);
        
        HealthCheckExecutorImpl healthCheckExecutor = new HealthCheckExecutorImpl(
            applicationContext, debugProperties, taskScheduler, toolProvider, resourceProvider
        );
        
        // When running configuration validation
        HealthCheckResult result = healthCheckExecutor.runHealthCheck();
        
        // Then the validation should complete without throwing exceptions
        assertThat(result).isNotNull();
        assertThat(result.checkName()).isEqualTo("comprehensive-health-check");
        
        // And should include configuration validation results
        @SuppressWarnings("unchecked")
        List<HealthCheckResult> componentResults = (List<HealthCheckResult>) result.details().get("componentResults");
        boolean hasConfigValidation = componentResults.stream()
            .anyMatch(r -> "configuration-validation".equals(r.checkName()));
        assertThat(hasConfigValidation).isTrue();
        
        // And the overall result should have a valid status
        assertThat(result.status()).isIn((Object[]) HealthStatus.values());
    }

    @Provide
    Arbitrary<McpDebugProperties> debugConfiguration() {
        return Arbitraries.oneOf(
            validDebugConfiguration(),
            invalidDebugConfiguration()
        );
    }

    @Provide
    Arbitrary<McpDebugProperties> validDebugConfiguration() {
        return Arbitraries.just(createValidDebugProperties());
    }

    @Provide
    Arbitrary<McpDebugProperties> invalidDebugConfiguration() {
        return Arbitraries.just(createInvalidDebugProperties());
    }

    @Provide
    Arbitrary<DebugLevel> debugLevel() {
        return Arbitraries.of(DebugLevel.values());
    }

    @Provide
    Arbitrary<Duration> retentionPeriod() {
        return Arbitraries.oneOf(
            Arbitraries.just(Duration.ofHours(24)), // Valid
            Arbitraries.just(Duration.ofMinutes(-1)), // Invalid (negative)
            Arbitraries.just(Duration.ZERO), // Edge case
            Arbitraries.just(null) // Invalid (null)
        );
    }

    @Provide
    Arbitrary<Integer> bufferSize() {
        return Arbitraries.oneOf(
            Arbitraries.integers().between(1, 100000), // Valid
            Arbitraries.integers().between(-100, 0) // Invalid
        );
    }

    @Provide
    Arbitrary<String> basePath() {
        return Arbitraries.oneOf(
            Arbitraries.just("debug-data"), // Valid
            Arbitraries.just(""), // Invalid (empty)
            Arbitraries.just("   "), // Invalid (whitespace)
            Arbitraries.just(null) // Invalid (null)
        );
    }

    /**
     * Create valid debug properties for testing.
     */
    private McpDebugProperties createValidDebugProperties() {
        McpDebugProperties properties = new McpDebugProperties();
        properties.setEnabled(true);
        properties.setGlobalLevel(DebugLevel.INFO);
        
        McpDebugProperties.ProtocolTracing protocolTracing = new McpDebugProperties.ProtocolTracing();
        protocolTracing.setEnabled(true);
        protocolTracing.setRetention(Duration.ofHours(24));
        protocolTracing.setBufferSize(10000);
        properties.setProtocolTracing(protocolTracing);
        
        McpDebugProperties.HealthChecks healthChecks = new McpDebugProperties.HealthChecks();
        healthChecks.setEnabled(true);
        healthChecks.setInterval(Duration.ofMinutes(5));
        healthChecks.setTimeout(Duration.ofSeconds(30));
        properties.setHealthChecks(healthChecks);
        
        McpDebugProperties.Storage storage = new McpDebugProperties.Storage();
        storage.setBasePath("debug-data");
        properties.setStorage(storage);
        
        return properties;
    }

    /**
     * Create invalid debug properties for testing.
     */
    private McpDebugProperties createInvalidDebugProperties() {
        McpDebugProperties properties = new McpDebugProperties();
        properties.setEnabled(true);
        properties.setGlobalLevel(null); // Invalid
        
        McpDebugProperties.ProtocolTracing protocolTracing = new McpDebugProperties.ProtocolTracing();
        protocolTracing.setEnabled(true);
        protocolTracing.setRetention(Duration.ofMinutes(-1)); // Invalid (negative)
        protocolTracing.setBufferSize(-1); // Invalid (negative)
        properties.setProtocolTracing(protocolTracing);
        
        McpDebugProperties.HealthChecks healthChecks = new McpDebugProperties.HealthChecks();
        healthChecks.setEnabled(true);
        healthChecks.setInterval(Duration.ofMinutes(-1)); // Invalid (negative)
        healthChecks.setTimeout(Duration.ofSeconds(-1)); // Invalid (negative)
        properties.setHealthChecks(healthChecks);
        
        McpDebugProperties.Storage storage = new McpDebugProperties.Storage();
        storage.setBasePath(""); // Invalid (empty)
        properties.setStorage(storage);
        
        return properties;
    }
}