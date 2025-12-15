package io.sandboxdev.gitmcp.debug.processor;

import io.sandboxdev.gitmcp.debug.model.PerformanceReport;
import io.sandboxdev.gitmcp.debug.model.Threshold;
import io.sandboxdev.gitmcp.debug.model.TimeRange;

import java.time.Duration;
import java.util.Map;

/**
 * Interface for aggregating performance metrics from MCP operations.
 * Handles metric collection, aggregation, and alert threshold monitoring.
 */
public interface MetricsAggregator {
    
    /**
     * Record a tool invocation with its response time.
     * 
     * @param toolName the name of the invoked tool
     * @param responseTime the time taken to process the tool invocation
     */
    void recordToolInvocation(String toolName, Duration responseTime);
    
    /**
     * Record a resource access with its response size.
     * 
     * @param resourceId the identifier of the accessed resource
     * @param responseSize the size of the response in bytes
     */
    void recordResourceAccess(String resourceId, long responseSize);
    
    /**
     * Generate a performance report for the specified time range.
     * 
     * @param timeRange the time range for the report
     * @return performance report containing aggregated metrics
     */
    PerformanceReport generateReport(TimeRange timeRange);
    
    /**
     * Set alert thresholds for various metrics.
     * 
     * @param thresholds map of metric names to their threshold values
     */
    void setAlertThresholds(Map<String, Threshold> thresholds);
}