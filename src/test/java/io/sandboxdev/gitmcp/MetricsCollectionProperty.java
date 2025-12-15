package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.interceptor.PerformanceMetricsCollector;
import io.sandboxdev.gitmcp.debug.model.PerformanceMetrics;
import io.sandboxdev.gitmcp.debug.processor.MetricsAggregator;
import io.sandboxdev.gitmcp.debug.processor.MetricsAggregatorImpl;
import net.jqwik.api.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.springaicommunity.mcp.annotation.McpTool;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based test for performance metrics collection functionality.
 * 
 * Feature: mcp-debugging, Property 3: Metrics collection completeness
 * Validates: Requirements 2.1, 2.2
 */
public class MetricsCollectionProperty {

    private final MetricsAggregator metricsAggregator = new MetricsAggregatorImpl();
    private final PerformanceMetricsCollector collector = new PerformanceMetricsCollector(metricsAggregator);

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 3: Metrics collection completeness
    void metricsCollectionCompleteness(@ForAll("toolNames") String toolName,
                                      @ForAll("responseSizes") long responseSize) throws Throwable {
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create mock join point for tool invocation
        ProceedingJoinPoint joinPoint = createMockJoinPoint(toolName, responseSize);
        McpTool mcpTool = mock(McpTool.class);
        
        Instant beforeInvocation = Instant.now();
        
        // When: Collector processes the tool invocation
        Object result = collector.collectPerformanceMetrics(joinPoint, mcpTool);
        
        Instant afterInvocation = Instant.now();
        
        // Then: Performance metrics should be collected
        List<PerformanceMetrics> metrics = collector.getAllMetrics();
        assertThat(metrics).hasSize(1);
        
        PerformanceMetrics metric = metrics.get(0);
        
        // Verify all required metric components are present
        assertThat(metric.toolName()).isEqualTo(toolName);
        assertThat(metric.responseTime()).isNotNull().isPositive();
        assertThat(metric.responseSize()).isEqualTo(responseSize);
        assertThat(metric.timestamp()).isNotNull()
            .isBetween(beforeInvocation, afterInvocation);
        assertThat(metric.success()).isTrue();
        
        // Verify result is returned unchanged
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Map.class);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 3: Metrics collection completeness
    void failedToolInvocationsAreTracked(@ForAll("toolNames") String toolName) throws Throwable {
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create mock join point that throws exception
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        McpTool mcpTool = mock(McpTool.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-path"});
        
        RuntimeException testException = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(testException);
        
        // When: Collector processes the failing tool invocation
        try {
            collector.collectPerformanceMetrics(joinPoint, mcpTool);
        } catch (RuntimeException e) {
            // Expected - collector should re-throw the exception
            assertThat(e).isEqualTo(testException);
        }
        
        // Then: Performance metrics should still be collected for failed invocation
        List<PerformanceMetrics> metrics = collector.getAllMetrics();
        assertThat(metrics).hasSize(1);
        
        PerformanceMetrics metric = metrics.get(0);
        assertThat(metric.toolName()).isEqualTo(toolName);
        assertThat(metric.responseTime()).isNotNull().isPositive();
        assertThat(metric.success()).isFalse();
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 3: Metrics collection completeness
    void multipleInvocationsAreAllTracked(@ForAll("toolNames") String toolName1,
                                         @ForAll("toolNames") String toolName2,
                                         @ForAll("responseSizes") long responseSize1,
                                         @ForAll("responseSizes") long responseSize2) throws Throwable {
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create mock join points for multiple tool invocations
        ProceedingJoinPoint joinPoint1 = createMockJoinPoint(toolName1, responseSize1);
        ProceedingJoinPoint joinPoint2 = createMockJoinPoint(toolName2, responseSize2);
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Collector processes multiple tool invocations
        collector.collectPerformanceMetrics(joinPoint1, mcpTool);
        collector.collectPerformanceMetrics(joinPoint2, mcpTool);
        
        // Then: All invocations should be tracked
        List<PerformanceMetrics> metrics = collector.getAllMetrics();
        assertThat(metrics).hasSize(2);
        
        // Verify each metric has required components
        for (PerformanceMetrics metric : metrics) {
            assertThat(metric.toolName()).isIn(toolName1, toolName2);
            assertThat(metric.responseTime()).isNotNull().isPositive();
            assertThat(metric.responseSize()).isIn(responseSize1, responseSize2);
            assertThat(metric.timestamp()).isNotNull();
            assertThat(metric.success()).isTrue();
        }
        
        // Verify metrics have different timestamps (assuming they don't execute at exactly the same nanosecond)
        if (metrics.size() == 2) {
            assertThat(metrics.get(0).timestamp()).isNotEqualTo(metrics.get(1).timestamp());
        }
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 3: Metrics collection completeness
    void toolSpecificMetricsFiltering(@ForAll("toolNames") String targetTool,
                                     @ForAll("toolNames") String otherTool,
                                     @ForAll("responseSizes") long responseSize) throws Throwable {
        Assume.that(!targetTool.equals(otherTool));
        
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create metrics for different tools
        ProceedingJoinPoint joinPoint1 = createMockJoinPoint(targetTool, responseSize);
        ProceedingJoinPoint joinPoint2 = createMockJoinPoint(otherTool, responseSize);
        McpTool mcpTool = mock(McpTool.class);
        
        collector.collectPerformanceMetrics(joinPoint1, mcpTool);
        collector.collectPerformanceMetrics(joinPoint2, mcpTool);
        
        // When: Get metrics for specific tool
        List<PerformanceMetrics> targetToolMetrics = collector.getMetricsForTool(targetTool);
        List<PerformanceMetrics> otherToolMetrics = collector.getMetricsForTool(otherTool);
        
        // Then: Only metrics for the specified tool should be returned
        assertThat(targetToolMetrics).hasSize(1);
        assertThat(targetToolMetrics.get(0).toolName()).isEqualTo(targetTool);
        
        assertThat(otherToolMetrics).hasSize(1);
        assertThat(otherToolMetrics.get(0).toolName()).isEqualTo(otherTool);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 3: Metrics collection completeness
    void timeRangeMetricsFiltering(@ForAll("toolNames") String toolName,
                                  @ForAll("responseSizes") long responseSize) throws Throwable {
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        Instant now = Instant.now();
        Instant past = now.minusSeconds(3600); // 1 hour ago
        Instant future = now.plusSeconds(3600); // 1 hour from now
        
        // Create a metric (it will have current timestamp)
        ProceedingJoinPoint joinPoint = createMockJoinPoint(toolName, responseSize);
        McpTool mcpTool = mock(McpTool.class);
        
        collector.collectPerformanceMetrics(joinPoint, mcpTool);
        
        // When: Get metrics for different time ranges
        List<PerformanceMetrics> pastMetrics = collector.getMetricsInTimeRange(past, past.plusSeconds(1800));
        List<PerformanceMetrics> presentMetrics = collector.getMetricsInTimeRange(past, future);
        List<PerformanceMetrics> futureMetrics = collector.getMetricsInTimeRange(future, future.plusSeconds(1800));
        
        // Then: Metrics should be filtered correctly by time range
        assertThat(pastMetrics).isEmpty(); // No metrics in the past
        assertThat(presentMetrics).hasSize(1); // Our metric is in the present range
        assertThat(futureMetrics).isEmpty(); // No metrics in the future
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 3: Metrics collection completeness
    void successFailureMetricsFiltering(@ForAll("toolNames") String toolName) throws Throwable {
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create successful and failed invocations
        ProceedingJoinPoint successJoinPoint = createMockJoinPoint(toolName, 1024L);
        ProceedingJoinPoint failJoinPoint = createMockFailingJoinPoint(toolName);
        McpTool mcpTool = mock(McpTool.class);
        
        collector.collectPerformanceMetrics(successJoinPoint, mcpTool);
        
        try {
            collector.collectPerformanceMetrics(failJoinPoint, mcpTool);
        } catch (RuntimeException e) {
            // Expected for failing invocation
        }
        
        // When: Get successful and failed metrics
        List<PerformanceMetrics> successfulMetrics = collector.getSuccessfulMetrics();
        List<PerformanceMetrics> failedMetrics = collector.getFailedMetrics();
        
        // Then: Metrics should be correctly separated by success status
        assertThat(successfulMetrics).hasSize(1);
        assertThat(successfulMetrics.get(0).success()).isTrue();
        
        assertThat(failedMetrics).hasSize(1);
        assertThat(failedMetrics.get(0).success()).isFalse();
    }

    private ProceedingJoinPoint createMockJoinPoint(String toolName, long responseSize) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-path"});
        
        // Create mock result with appropriate size
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("success", true);
        mockResult.put("data", "x".repeat((int) Math.min(responseSize / 2, 1000))); // Approximate size
        when(joinPoint.proceed()).thenReturn(mockResult);
        
        return joinPoint;
    }

    private ProceedingJoinPoint createMockFailingJoinPoint(String toolName) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-path"});
        
        RuntimeException testException = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(testException);
        
        return joinPoint;
    }

    @Provide
    Arbitrary<String> toolNames() {
        return Arbitraries.of(
            "getStatus",
            "getCurrentBranch",
            "listBranches",
            "getHistory",
            "createCommit",
            "stageFiles"
        );
    }

    @Provide
    Arbitrary<Long> responseSizes() {
        return Arbitraries.longs().between(0L, 10000L);
    }
}