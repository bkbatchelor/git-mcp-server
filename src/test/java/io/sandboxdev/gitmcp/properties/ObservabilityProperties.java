package io.sandboxdev.gitmcp.properties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.sandboxdev.gitmcp.config.ObservabilityConfiguration;
import io.sandboxdev.gitmcp.observability.ObservabilityService;
import net.jqwik.api.*;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for observability and metrics.
 * Tests Property 14: Observability (Requirements 13.1, 13.2, 13.3, 13.5)
 */
class ObservabilityProperties {

    private final ObservabilityConfiguration config = new ObservabilityConfiguration();
    private final ObservabilityService observabilityService = new ObservabilityService(config.meterRegistry());

    /**
     Property 14: Observability (Req 13.1)
     Micrometer Tracing integration for distributed tracing
     */
    @Property
    void micrometerTracingIntegrationWorks(@ForAll("gitOperations") GitOperation operation) {
        ObservabilityService.MockSpan span = createDistributedTrace(operation);
        
        assertThat(span).isNotNull();
        assertThat(span.context().traceId()).isNotBlank();
        assertThat(span.context().spanId()).isNotBlank();
    }

    /**
     Property 14: Observability (Req 13.2)
     Git operation metrics for latency and success/failure rates
     */
    @Property
    void gitOperationMetricsTracked(@ForAll("gitOperations") GitOperation operation,
                                   @ForAll("operationResults") OperationResult result) {
        recordGitOperationMetrics(operation, result);
        
        Timer timer = getGitOperationTimer(operation.type());
        Counter successCounter = getGitOperationCounter(operation.type(), "success");
        Counter failureCounter = getGitOperationCounter(operation.type(), "failure");
        
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isGreaterThan(0);
        
        if (result.success()) {
            assertThat(successCounter.count()).isGreaterThan(0);
        } else {
            assertThat(failureCounter.count()).isGreaterThan(0);
        }
    }

    /**
     Property 14: Observability (Req 13.3)
     Spring AI token usage tracking via gen_ai.client.token.usage metrics
     */
    @Property
    void springAiTokenUsageTracked(@ForAll("aiOperations") AiOperation aiOperation) {
        recordAiTokenUsage(aiOperation);
        
        Counter tokenUsageCounter = getTokenUsageCounter();
        assertThat(tokenUsageCounter).isNotNull();
        assertThat(tokenUsageCounter.count()).isGreaterThan(0);
        
        // Verify token usage tags
        Map<String, String> tags = getTokenUsageTags(tokenUsageCounter);
        assertThat(tags).containsKey("model");
        assertThat(tags).containsKey("operation");
    }

    /**
     Property 14: Observability (Req 13.4, 13.5)
     Health and info endpoints exposed with production-safe logging
     */
    @Property
    void healthAndInfoEndpointsExposed(@ForAll("environmentTypes") String environment) {
        boolean healthEndpointAvailable = isHealthEndpointAvailable();
        boolean infoEndpointAvailable = isInfoEndpointAvailable();
        boolean productionLoggingSafe = isProductionLoggingSafe(environment);
        
        assertThat(healthEndpointAvailable).isTrue();
        assertThat(infoEndpointAvailable).isTrue();
        
        if ("production".equals(environment)) {
            assertThat(productionLoggingSafe).isTrue();
        }
    }

    // Generators for property-based testing

    @Provide
    Arbitrary<GitOperation> gitOperations() {
        return Arbitraries.oneOf(
                Arbitraries.just(new GitOperation("status", Duration.ofMillis(100))),
                Arbitraries.just(new GitOperation("commit", Duration.ofMillis(500))),
                Arbitraries.just(new GitOperation("diff", Duration.ofMillis(300))),
                Arbitraries.just(new GitOperation("branch", Duration.ofMillis(200))),
                Arbitraries.just(new GitOperation("log", Duration.ofMillis(400)))
        );
    }

    @Provide
    Arbitrary<OperationResult> operationResults() {
        return Arbitraries.oneOf(
                Arbitraries.just(new OperationResult(true, "Operation completed successfully")),
                Arbitraries.just(new OperationResult(false, "Repository not found")),
                Arbitraries.just(new OperationResult(false, "Permission denied")),
                Arbitraries.just(new OperationResult(true, "No changes detected"))
        );
    }

    @Provide
    Arbitrary<AiOperation> aiOperations() {
        return Arbitraries.oneOf(
                Arbitraries.just(new AiOperation("gpt-4", "completion", 150, 50)),
                Arbitraries.just(new AiOperation("gpt-3.5-turbo", "chat", 100, 30)),
                Arbitraries.just(new AiOperation("claude-3", "analysis", 200, 75))
        );
    }

    @Provide
    Arbitrary<String> environmentTypes() {
        return Arbitraries.of("development", "staging", "production");
    }

    // Helper methods that use ObservabilityService (GREEN phase)

    private ObservabilityService.MockSpan createDistributedTrace(GitOperation operation) {
        return observabilityService.createDistributedTrace(operation.type());
    }

    private void recordGitOperationMetrics(GitOperation operation, OperationResult result) {
        observabilityService.recordGitOperationMetrics(operation.type(), operation.duration(), result.success());
    }

    private Timer getGitOperationTimer(String operationType) {
        return observabilityService.getGitOperationTimer(operationType);
    }

    private Counter getGitOperationCounter(String operationType, String resultType) {
        return observabilityService.getGitOperationCounter(operationType, resultType);
    }

    private void recordAiTokenUsage(AiOperation aiOperation) {
        observabilityService.recordAiTokenUsage(aiOperation.model(), aiOperation.operation(), 
                aiOperation.inputTokens(), aiOperation.outputTokens());
    }

    private Counter getTokenUsageCounter() {
        return observabilityService.getTokenUsageCounter();
    }

    private Map<String, String> getTokenUsageTags(Counter counter) {
        return observabilityService.getTokenUsageTags(counter);
    }

    private boolean isHealthEndpointAvailable() {
        return observabilityService.isHealthEndpointAvailable();
    }

    private boolean isInfoEndpointAvailable() {
        return observabilityService.isInfoEndpointAvailable();
    }

    private boolean isProductionLoggingSafe(String environment) {
        return observabilityService.isProductionLoggingSafe(environment);
    }

    // Test data records
    public record GitOperation(String type, Duration duration) {}
    public record OperationResult(boolean success, String message) {}
    public record AiOperation(String model, String operation, int inputTokens, int outputTokens) {}
}
