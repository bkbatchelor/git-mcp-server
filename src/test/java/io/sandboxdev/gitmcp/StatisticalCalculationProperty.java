package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.interceptor.PerformanceMetricsCollector;
import io.sandboxdev.gitmcp.debug.processor.MetricsAggregatorImpl;
import net.jqwik.api.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.springaicommunity.mcp.annotation.McpTool;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

/**
 * Property-based test for statistical calculation accuracy.
 * 
 * Feature: mcp-debugging, Property 4: Statistical calculation accuracy
 * Validates: Requirements 2.3
 */
public class StatisticalCalculationProperty {

    private final PerformanceMetricsCollector collector = new PerformanceMetricsCollector(new MetricsAggregatorImpl());

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 4: Statistical calculation accuracy
    void averageResponseTimeCalculationAccuracy(@ForAll("toolNames") String toolName,
                                               @ForAll("responseTimes") List<Long> responseTimesMs) throws Throwable {
        Assume.that(responseTimesMs.size() >= 2);
        Assume.that(responseTimesMs.stream().allMatch(time -> time > 0));
        
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create multiple invocations with known response times
        for (Long responseTimeMs : responseTimesMs) {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithDelay(toolName, responseTimeMs);
            McpTool mcpTool = mock(McpTool.class);
            collector.collectPerformanceMetrics(joinPoint, mcpTool);
        }
        
        // When: Get tool statistics
        PerformanceMetricsCollector.ToolStatistics stats = collector.getToolStatistics(toolName);
        
        // Then: Average should be mathematically correct
        double expectedAverage = responseTimesMs.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long actualAverageMs = stats.avgResponseTime().toMillis();
        
        // Allow for small timing variations (within 10% or 10ms, whichever is larger)
        long tolerance = Math.max(10L, (long) (expectedAverage * 0.1));
        
        assertThat(actualAverageMs)
            .as("Average response time should be mathematically correct")
            .isBetween((long) expectedAverage - tolerance, (long) expectedAverage + tolerance);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 4: Statistical calculation accuracy
    void minMaxResponseTimeCalculationAccuracy(@ForAll("toolNames") String toolName,
                                              @ForAll("responseTimes") List<Long> responseTimesMs) throws Throwable {
        Assume.that(responseTimesMs.size() >= 2);
        Assume.that(responseTimesMs.stream().allMatch(time -> time > 0));
        
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create multiple invocations with known response times
        for (Long responseTimeMs : responseTimesMs) {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithDelay(toolName, responseTimeMs);
            McpTool mcpTool = mock(McpTool.class);
            collector.collectPerformanceMetrics(joinPoint, mcpTool);
        }
        
        // When: Get tool statistics
        PerformanceMetricsCollector.ToolStatistics stats = collector.getToolStatistics(toolName);
        
        // Then: Min and max should be mathematically correct
        long expectedMin = responseTimesMs.stream().mapToLong(Long::longValue).min().orElse(0L);
        long expectedMax = responseTimesMs.stream().mapToLong(Long::longValue).max().orElse(0L);
        
        long actualMinMs = stats.minResponseTime().toMillis();
        long actualMaxMs = stats.maxResponseTime().toMillis();
        
        // Allow for small timing variations
        long minTolerance = Math.max(5L, expectedMin / 10);
        long maxTolerance = Math.max(5L, expectedMax / 10);
        
        assertThat(actualMinMs)
            .as("Minimum response time should be mathematically correct")
            .isBetween(expectedMin - minTolerance, expectedMin + minTolerance);
            
        assertThat(actualMaxMs)
            .as("Maximum response time should be mathematically correct")
            .isBetween(expectedMax - maxTolerance, expectedMax + maxTolerance);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 4: Statistical calculation accuracy
    void percentile95CalculationAccuracy(@ForAll("toolNames") String toolName,
                                        @ForAll("responseTimes") List<Long> responseTimesMs) throws Throwable {
        Assume.that(responseTimesMs.size() >= 10); // Need enough data for meaningful percentile
        Assume.that(responseTimesMs.stream().allMatch(time -> time > 0));
        
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create multiple invocations with known response times
        for (Long responseTimeMs : responseTimesMs) {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithDelay(toolName, responseTimeMs);
            McpTool mcpTool = mock(McpTool.class);
            collector.collectPerformanceMetrics(joinPoint, mcpTool);
        }
        
        // When: Get tool statistics
        PerformanceMetricsCollector.ToolStatistics stats = collector.getToolStatistics(toolName);
        
        // Then: 95th percentile should be mathematically correct
        List<Long> sortedTimes = responseTimesMs.stream().sorted().toList();
        int p95Index = (int) Math.ceil(0.95 * sortedTimes.size()) - 1;
        long expectedP95 = sortedTimes.get(Math.max(0, p95Index));
        
        long actualP95Ms = stats.p95ResponseTime().toMillis();
        
        // Allow for timing variations
        long tolerance = Math.max(5L, expectedP95 / 10);
        
        assertThat(actualP95Ms)
            .as("95th percentile response time should be mathematically correct")
            .isBetween(expectedP95 - tolerance, expectedP95 + tolerance);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 4: Statistical calculation accuracy
    void invocationCountAccuracy(@ForAll("toolNames") String toolName,
                                @ForAll("responseTimes") List<Long> responseTimesMs) throws Throwable {
        Assume.that(responseTimesMs.size() >= 1);
        Assume.that(responseTimesMs.size() <= 50); // Keep it reasonable
        
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create multiple invocations
        for (Long responseTimeMs : responseTimesMs) {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithDelay(toolName, responseTimeMs);
            McpTool mcpTool = mock(McpTool.class);
            collector.collectPerformanceMetrics(joinPoint, mcpTool);
        }
        
        // When: Get tool statistics
        PerformanceMetricsCollector.ToolStatistics stats = collector.getToolStatistics(toolName);
        
        // Then: Invocation count should be exact
        assertThat(stats.invocationCount())
            .as("Invocation count should be exact")
            .isEqualTo(responseTimesMs.size());
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 4: Statistical calculation accuracy
    void successRateCalculationAccuracy(@ForAll("toolNames") String toolName,
                                       @ForAll("successFailureCounts") SuccessFailureCounts counts) throws Throwable {
        Assume.that(counts.successCount() + counts.failureCount() >= 1);
        
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        McpTool mcpTool = mock(McpTool.class);
        
        // Create successful invocations
        for (int i = 0; i < counts.successCount(); i++) {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithDelay(toolName, 100L);
            collector.collectPerformanceMetrics(joinPoint, mcpTool);
        }
        
        // Create failed invocations
        for (int i = 0; i < counts.failureCount(); i++) {
            ProceedingJoinPoint joinPoint = createMockFailingJoinPoint(toolName);
            try {
                collector.collectPerformanceMetrics(joinPoint, mcpTool);
            } catch (RuntimeException e) {
                // Expected for failing invocations
            }
        }
        
        // When: Get tool statistics
        PerformanceMetricsCollector.ToolStatistics stats = collector.getToolStatistics(toolName);
        
        // Then: Success rate should be mathematically correct
        double expectedSuccessRate = (double) counts.successCount() / (counts.successCount() + counts.failureCount());
        double actualSuccessRate = stats.successRate();
        
        assertThat(actualSuccessRate)
            .as("Success rate should be mathematically correct")
            .isCloseTo(expectedSuccessRate, within(0.001)); // Allow for floating point precision
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 4: Statistical calculation accuracy
    void totalResponseSizeCalculationAccuracy(@ForAll("toolNames") String toolName,
                                             @ForAll("responseSizes") List<Long> responseSizes) throws Throwable {
        Assume.that(responseSizes.size() >= 1);
        Assume.that(responseSizes.stream().allMatch(size -> size >= 0));
        
        // Given: Clear any existing metrics
        collector.clearMetrics();
        
        // Create multiple invocations with known response sizes
        for (Long responseSize : responseSizes) {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithSize(toolName, responseSize);
            McpTool mcpTool = mock(McpTool.class);
            collector.collectPerformanceMetrics(joinPoint, mcpTool);
        }
        
        // When: Get tool statistics
        PerformanceMetricsCollector.ToolStatistics stats = collector.getToolStatistics(toolName);
        
        // Then: Total response size should be mathematically correct
        long expectedTotal = responseSizes.stream().mapToLong(Long::longValue).sum();
        long actualTotal = stats.totalResponseSize();
        
        // Allow for small variations due to string encoding approximations
        long tolerance = Math.max(10L, expectedTotal / 100);
        
        assertThat(actualTotal)
            .as("Total response size should be mathematically correct")
            .isBetween(expectedTotal - tolerance, expectedTotal + tolerance);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 4: Statistical calculation accuracy
    void emptyDataSetHandling(@ForAll("toolNames") String toolName) {
        // Given: Clear any existing metrics (no invocations)
        collector.clearMetrics();
        
        // When: Get statistics for tool with no invocations
        PerformanceMetricsCollector.ToolStatistics stats = collector.getToolStatistics(toolName);
        
        // Then: Statistics should handle empty data set gracefully
        assertThat(stats.toolName()).isEqualTo(toolName);
        assertThat(stats.invocationCount()).isEqualTo(0);
        assertThat(stats.minResponseTime()).isEqualTo(Duration.ZERO);
        assertThat(stats.maxResponseTime()).isEqualTo(Duration.ZERO);
        assertThat(stats.avgResponseTime()).isEqualTo(Duration.ZERO);
        assertThat(stats.p95ResponseTime()).isEqualTo(Duration.ZERO);
        assertThat(stats.successRate()).isEqualTo(0.0);
        assertThat(stats.totalResponseSize()).isEqualTo(0L);
    }

    private ProceedingJoinPoint createMockJoinPointWithDelay(String toolName, long delayMs) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-path"});
        
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("success", true);
        
        // Simulate processing delay
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(delayMs);
            return mockResult;
        });
        
        return joinPoint;
    }

    private ProceedingJoinPoint createMockJoinPointWithSize(String toolName, long responseSize) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-path"});
        
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("success", true);
        // Create data of approximately the specified size
        mockResult.put("data", "x".repeat((int) Math.min(responseSize / 2, 10000)));
        
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
            "getHistory"
        );
    }

    @Provide
    Arbitrary<List<Long>> responseTimes() {
        return Arbitraries.longs().between(1L, 1000L)
            .list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<Long>> responseSizes() {
        return Arbitraries.longs().between(0L, 5000L)
            .list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<SuccessFailureCounts> successFailureCounts() {
        return Combinators.combine(
            Arbitraries.integers().between(0, 20),
            Arbitraries.integers().between(0, 20)
        ).as((Integer success, Integer failure) -> new SuccessFailureCounts(success, failure));
    }

    public record SuccessFailureCounts(int successCount, int failureCount) {}
}