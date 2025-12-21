package io.sandboxdev.gitmcp.debug.interceptor;

import io.sandboxdev.gitmcp.debug.model.PerformanceMetrics;
import io.sandboxdev.gitmcp.debug.processor.MetricsAggregator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AOP interceptor for collecting performance metrics from MCP tool invocations.
 * 
 * This aspect intercepts all methods annotated with @McpTool to collect:
 * - Response time tracking for tool invocations
 * - Resource access patterns and response sizes
 * - Statistical metrics (percentiles, averages, peaks)
 */
@Aspect
@Component
@ConditionalOnProperty(name = "mcp-debug.performance-monitoring.enabled", havingValue = "true", matchIfMissing = false)
public class PerformanceMetricsCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsCollector.class);
    
    private final MetricsAggregator metricsAggregator;
    private final List<PerformanceMetrics> collectedMetrics = new CopyOnWriteArrayList<>();
    
    public PerformanceMetricsCollector(MetricsAggregator metricsAggregator) {
        this.metricsAggregator = metricsAggregator;
    }
    
    /**
     * Intercepts all @McpTool annotated methods to collect performance metrics.
     */
    @Around("@annotation(mcpTool)")
    public Object collectPerformanceMetrics(ProceedingJoinPoint joinPoint, McpTool mcpTool) throws Throwable {
        String toolName = joinPoint.getSignature().getName();
        Instant startTime = Instant.now();
        
        logger.debug("Starting performance measurement for tool: {}", toolName);
        
        Object result = null;
        boolean success = false;
        long responseSize = 0;
        
        try {
            // Execute the original method
            result = joinPoint.proceed();
            success = true;
            
            // Calculate response size
            responseSize = calculateResponseSize(result);
            
        } catch (Exception e) {
            success = false;
            logger.debug("Tool invocation failed for performance measurement: {} - {}", toolName, e.getMessage());
            throw e; // Re-throw to maintain original behavior
            
        } finally {
            // Calculate processing time
            Duration responseTime = Duration.between(startTime, Instant.now());
            
            // Create performance metrics
            PerformanceMetrics metrics = new PerformanceMetrics(
                toolName,
                responseTime,
                responseSize,
                startTime,
                success
            );
            
            // Store metrics locally
            collectedMetrics.add(metrics);
            
            // Send to aggregator for processing
            metricsAggregator.recordToolInvocation(toolName, responseTime);
            if (responseSize > 0) {
                metricsAggregator.recordResourceAccess(toolName, responseSize);
            }
            
            logger.debug("Collected performance metrics for tool: {} - {}ms, {} bytes, success: {}", 
                toolName, responseTime.toMillis(), responseSize, success);
        }
        
        return result;
    }
    
    /**
     * Get all collected performance metrics.
     */
    public List<PerformanceMetrics> getAllMetrics() {
        return List.copyOf(collectedMetrics);
    }
    
    /**
     * Get performance metrics for a specific tool.
     */
    public List<PerformanceMetrics> getMetricsForTool(String toolName) {
        return collectedMetrics.stream()
            .filter(metrics -> metrics.toolName().equals(toolName))
            .toList();
    }
    
    /**
     * Get performance metrics within a time range.
     */
    public List<PerformanceMetrics> getMetricsInTimeRange(Instant startTime, Instant endTime) {
        return collectedMetrics.stream()
            .filter(metrics -> !metrics.timestamp().isBefore(startTime) && 
                              !metrics.timestamp().isAfter(endTime))
            .toList();
    }
    
    /**
     * Get successful performance metrics only.
     */
    public List<PerformanceMetrics> getSuccessfulMetrics() {
        return collectedMetrics.stream()
            .filter(PerformanceMetrics::success)
            .toList();
    }
    
    /**
     * Get failed performance metrics only.
     */
    public List<PerformanceMetrics> getFailedMetrics() {
        return collectedMetrics.stream()
            .filter(metrics -> !metrics.success())
            .toList();
    }
    
    /**
     * Clear all collected metrics.
     */
    public void clearMetrics() {
        collectedMetrics.clear();
        logger.debug("Cleared all performance metrics");
    }
    
    /**
     * Get basic statistics for a tool.
     */
    public ToolStatistics getToolStatistics(String toolName) {
        List<PerformanceMetrics> toolMetrics = getMetricsForTool(toolName);
        
        if (toolMetrics.isEmpty()) {
            return new ToolStatistics(toolName, 0, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0, 0L);
        }
        
        List<Duration> responseTimes = toolMetrics.stream()
            .map(PerformanceMetrics::responseTime)
            .sorted()
            .toList();
        
        Duration minTime = responseTimes.get(0);
        Duration maxTime = responseTimes.get(responseTimes.size() - 1);
        Duration avgTime = Duration.ofNanos(
            (long) responseTimes.stream()
                .mapToLong(Duration::toNanos)
                .average()
                .orElse(0)
        );
        
        // Calculate 95th percentile
        int p95Index = (int) Math.ceil(0.95 * responseTimes.size()) - 1;
        Duration p95Time = responseTimes.get(Math.max(0, p95Index));
        
        double successRate = (double) toolMetrics.stream()
            .mapToInt(m -> m.success() ? 1 : 0)
            .sum() / toolMetrics.size();
        
        long totalResponseSize = toolMetrics.stream()
            .mapToLong(PerformanceMetrics::responseSize)
            .sum();
        
        return new ToolStatistics(
            toolName,
            toolMetrics.size(),
            minTime,
            maxTime,
            avgTime,
            p95Time,
            successRate,
            totalResponseSize
        );
    }
    
    /**
     * Calculate the approximate size of a response object.
     */
    private long calculateResponseSize(Object response) {
        if (response == null) {
            return 0;
        }
        
        if (response instanceof String) {
            return ((String) response).length() * 2; // Approximate UTF-16 size
        }
        
        if (response instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) response;
            return map.toString().length() * 2; // Rough approximation
        }
        
        if (response instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) response;
            return list.toString().length() * 2; // Rough approximation
        }
        
        // For other objects, use string representation size
        return response.toString().length() * 2;
    }
    
    /**
     * Statistics for a specific tool.
     */
    public record ToolStatistics(
        String toolName,
        int invocationCount,
        Duration minResponseTime,
        Duration maxResponseTime,
        Duration avgResponseTime,
        Duration p95ResponseTime,
        double successRate,
        long totalResponseSize
    ) {}
}