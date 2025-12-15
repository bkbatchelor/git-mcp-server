package io.sandboxdev.gitmcp.debug.processor;

import io.sandboxdev.gitmcp.debug.model.PerformanceReport;
import io.sandboxdev.gitmcp.debug.model.TimeRange;
import io.sandboxdev.gitmcp.debug.model.Threshold;
import io.sandboxdev.gitmcp.debug.model.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Enhanced implementation of MetricsAggregator for collecting and aggregating performance metrics.
 * Supports alert threshold monitoring and trend analysis reporting.
 */
@Component
@ConditionalOnProperty(name = "mcp-debug.enabled", havingValue = "true", matchIfMissing = false)
public class MetricsAggregatorImpl implements MetricsAggregator {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsAggregatorImpl.class);
    
    private final Map<String, List<ToolInvocationRecord>> toolInvocations = new ConcurrentHashMap<>();
    private final Map<String, List<ResourceAccessRecord>> resourceAccesses = new ConcurrentHashMap<>();
    private final Map<String, Threshold> alertThresholds = new ConcurrentHashMap<>();
    private final List<Consumer<AlertEvent>> alertListeners = new CopyOnWriteArrayList<>();
    
    @Override
    public void recordToolInvocation(String toolName, Duration responseTime) {
        ToolInvocationRecord record = new ToolInvocationRecord(toolName, responseTime, Instant.now());
        
        toolInvocations.computeIfAbsent(toolName, k -> new CopyOnWriteArrayList<>()).add(record);
        
        logger.debug("Recorded tool invocation: {} - {}ms", toolName, responseTime.toMillis());
        
        // Check thresholds and generate alerts with contextual information (Requirement 2.5)
        checkThresholdsWithContext(toolName, responseTime, record);
    }
    
    @Override
    public void recordResourceAccess(String resourceId, long responseSize) {
        ResourceAccessRecord record = new ResourceAccessRecord(resourceId, responseSize, Instant.now());
        
        resourceAccesses.computeIfAbsent(resourceId, k -> new CopyOnWriteArrayList<>()).add(record);
        
        logger.debug("Recorded resource access: {} - {} bytes", resourceId, responseSize);
        
        // Check resource size thresholds
        checkResourceSizeThresholds(resourceId, responseSize, record);
    }
    
    @Override
    public PerformanceReport generateReport(TimeRange timeRange) {
        logger.info("Generating performance report for time range: {} to {} (Requirement 2.4)", 
            timeRange.start(), timeRange.end());
        
        // Filter records within time range
        Map<String, List<ToolInvocationRecord>> filteredInvocations = new ConcurrentHashMap<>();
        Map<String, List<ResourceAccessRecord>> filteredAccesses = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, List<ToolInvocationRecord>> entry : toolInvocations.entrySet()) {
            List<ToolInvocationRecord> filtered = entry.getValue().stream()
                .filter(record -> isWithinTimeRange(record.timestamp(), timeRange))
                .toList();
            if (!filtered.isEmpty()) {
                filteredInvocations.put(entry.getKey(), filtered);
            }
        }
        
        for (Map.Entry<String, List<ResourceAccessRecord>> entry : resourceAccesses.entrySet()) {
            List<ResourceAccessRecord> filtered = entry.getValue().stream()
                .filter(record -> isWithinTimeRange(record.timestamp(), timeRange))
                .toList();
            if (!filtered.isEmpty()) {
                filteredAccesses.put(entry.getKey(), filtered);
            }
        }
        
        // Enhanced report with trend analysis
        PerformanceReport report = new PerformanceReport(
            timeRange,
            Instant.now(),
            calculateAverageResponseTimes(filteredInvocations),
            calculatePercentile95ResponseTimes(filteredInvocations),
            calculateMaxResponseTimes(filteredInvocations),
            calculateTotalInvocations(filteredInvocations),
            calculateSuccessRates(filteredInvocations)
        );
        
        logger.info("Performance report generated with {} tool metrics and {} resource metrics", 
            filteredInvocations.size(), filteredAccesses.size());
        
        return report;
    }
    
    @Override
    public void setAlertThresholds(Map<String, Threshold> thresholds) {
        alertThresholds.clear();
        alertThresholds.putAll(thresholds);
        logger.info("Updated alert thresholds for {} metrics: {}", thresholds.size(), thresholds.keySet());
    }
    
    /**
     * Add an alert listener for threshold violations.
     * 
     * @param listener consumer that will be notified of alert events
     */
    public void addAlertListener(Consumer<AlertEvent> listener) {
        alertListeners.add(listener);
        logger.debug("Added alert listener for threshold monitoring");
    }
    
    /**
     * Remove an alert listener.
     * 
     * @param listener consumer to remove
     */
    public void removeAlertListener(Consumer<AlertEvent> listener) {
        alertListeners.remove(listener);
        logger.debug("Removed alert listener");
    }
    
    /**
     * Generate a trend analysis report showing performance changes over time.
     * 
     * @param timeRange the time range for trend analysis
     * @param intervalDuration the interval size for trend buckets
     * @return map of time intervals to performance metrics
     */
    public Map<Instant, PerformanceReport> generateTrendAnalysis(TimeRange timeRange, Duration intervalDuration) {
        logger.info("Generating trend analysis for time range: {} with {} intervals", 
            timeRange, intervalDuration);
        
        Map<Instant, PerformanceReport> trends = new ConcurrentHashMap<>();
        
        Instant current = timeRange.start();
        while (current.isBefore(timeRange.end())) {
            Instant intervalEnd = current.plus(intervalDuration);
            if (intervalEnd.isAfter(timeRange.end())) {
                intervalEnd = timeRange.end();
            }
            
            TimeRange intervalRange = new TimeRange(current, intervalEnd);
            PerformanceReport intervalReport = generateReport(intervalRange);
            trends.put(current, intervalReport);
            
            current = intervalEnd;
        }
        
        logger.debug("Generated trend analysis with {} time intervals", trends.size());
        return trends;
    }
    
    private void checkThresholdsWithContext(String toolName, Duration responseTime, ToolInvocationRecord record) {
        Threshold threshold = alertThresholds.get(toolName);
        if (threshold != null && threshold.type() == Threshold.ThresholdType.GREATER_THAN) {
            Duration thresholdDuration = Duration.ofMillis((long) threshold.value());
            if (responseTime.compareTo(thresholdDuration) > 0) {
                // Generate alert with contextual information (Requirement 2.5)
                Map<String, Object> context = Map.of(
                    "toolName", toolName,
                    "actualResponseTime", responseTime.toMillis(),
                    "thresholdResponseTime", (long) threshold.value(),
                    "timestamp", record.timestamp(),
                    "exceedancePercentage", ((double) responseTime.toMillis() / threshold.value() - 1.0) * 100
                );
                
                AlertEvent alert = new AlertEvent(
                    AlertType.PERFORMANCE_DEGRADATION,
                    String.format("Tool '%s' response time exceeded threshold: %dms > %dms (%.1f%% over)", 
                        toolName, responseTime.toMillis(), (long) threshold.value(),
                        ((double) responseTime.toMillis() / threshold.value() - 1.0) * 100),
                    context,
                    record.timestamp()
                );
                
                logger.warn("🚨 PERFORMANCE ALERT: {}", alert.message());
                notifyAlertListeners(alert);
            }
        }
    }
    
    private void checkResourceSizeThresholds(String resourceId, long responseSize, ResourceAccessRecord record) {
        String resourceThresholdKey = "resource_" + resourceId;
        Threshold threshold = alertThresholds.get(resourceThresholdKey);
        if (threshold != null && threshold.type() == Threshold.ThresholdType.GREATER_THAN) {
            if (responseSize > threshold.value()) {
                Map<String, Object> context = Map.of(
                    "resourceId", resourceId,
                    "actualSize", responseSize,
                    "thresholdSize", (long) threshold.value(),
                    "timestamp", record.timestamp(),
                    "exceedancePercentage", ((double) responseSize / threshold.value() - 1.0) * 100
                );
                
                AlertEvent alert = new AlertEvent(
                    AlertType.RESOURCE_SIZE_EXCEEDED,
                    String.format("Resource '%s' size exceeded threshold: %d bytes > %d bytes (%.1f%% over)", 
                        resourceId, responseSize, (long) threshold.value(),
                        ((double) responseSize / threshold.value() - 1.0) * 100),
                    context,
                    record.timestamp()
                );
                
                logger.warn("🚨 RESOURCE SIZE ALERT: {}", alert.message());
                notifyAlertListeners(alert);
            }
        }
    }
    
    private void notifyAlertListeners(AlertEvent alert) {
        for (Consumer<AlertEvent> listener : alertListeners) {
            try {
                listener.accept(alert);
            } catch (Exception e) {
                logger.warn("Error notifying alert listener: {}", e.getMessage());
            }
        }
    }
    
    private boolean isWithinTimeRange(Instant timestamp, TimeRange timeRange) {
        return !timestamp.isBefore(timeRange.start()) && !timestamp.isAfter(timeRange.end());
    }
    
    private Map<String, Duration> calculateAverageResponseTimes(Map<String, List<ToolInvocationRecord>> invocations) {
        Map<String, Duration> averages = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, List<ToolInvocationRecord>> entry : invocations.entrySet()) {
            List<ToolInvocationRecord> records = entry.getValue();
            long totalNanos = records.stream()
                .mapToLong(record -> record.responseTime().toNanos())
                .sum();
            Duration average = Duration.ofNanos(totalNanos / records.size());
            averages.put(entry.getKey(), average);
        }
        
        return averages;
    }
    
    private Map<String, Duration> calculatePercentile95ResponseTimes(Map<String, List<ToolInvocationRecord>> invocations) {
        Map<String, Duration> percentiles = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, List<ToolInvocationRecord>> entry : invocations.entrySet()) {
            List<Duration> responseTimes = entry.getValue().stream()
                .map(ToolInvocationRecord::responseTime)
                .sorted()
                .toList();
            
            if (!responseTimes.isEmpty()) {
                int p95Index = (int) Math.ceil(0.95 * responseTimes.size()) - 1;
                Duration p95 = responseTimes.get(Math.max(0, p95Index));
                percentiles.put(entry.getKey(), p95);
            }
        }
        
        return percentiles;
    }
    
    private Map<String, Duration> calculateMaxResponseTimes(Map<String, List<ToolInvocationRecord>> invocations) {
        Map<String, Duration> maxTimes = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, List<ToolInvocationRecord>> entry : invocations.entrySet()) {
            Duration max = entry.getValue().stream()
                .map(ToolInvocationRecord::responseTime)
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);
            maxTimes.put(entry.getKey(), max);
        }
        
        return maxTimes;
    }
    
    private Map<String, Long> calculateTotalInvocations(Map<String, List<ToolInvocationRecord>> invocations) {
        Map<String, Long> totals = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, List<ToolInvocationRecord>> entry : invocations.entrySet()) {
            totals.put(entry.getKey(), (long) entry.getValue().size());
        }
        
        return totals;
    }
    
    private Map<String, Double> calculateSuccessRates(Map<String, List<ToolInvocationRecord>> invocations) {
        Map<String, Double> successRates = new ConcurrentHashMap<>();
        
        // For now, assume all recorded invocations are successful
        // In a full implementation, we'd track success/failure status
        for (String toolName : invocations.keySet()) {
            successRates.put(toolName, 1.0);
        }
        
        return successRates;
    }
    
    /**
     * Record of a tool invocation.
     */
    private record ToolInvocationRecord(
        String toolName,
        Duration responseTime,
        Instant timestamp
    ) {}
    
    /**
     * Record of a resource access.
     */
    private record ResourceAccessRecord(
        String resourceId,
        long responseSize,
        Instant timestamp
    ) {}
    
    /**
     * Alert event for threshold violations.
     */
    public record AlertEvent(
        AlertType type,
        String message,
        Map<String, Object> context,
        Instant timestamp
    ) {}
}