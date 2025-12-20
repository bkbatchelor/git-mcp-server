package io.sandboxdev.gitmcp.debug.processor;

import io.sandboxdev.gitmcp.debug.model.PerformanceReport;
import io.sandboxdev.gitmcp.debug.model.TimeRange;
import io.sandboxdev.gitmcp.debug.model.Threshold;

import java.time.Duration;
import java.util.Map;

/**
 * Interface for aggregating performance metrics from MCP tool invocations.
 * 
 * Handles collection, aggregation, and reporting of performance data.
 */
public interface MetricsAggregator {
    
    /**
     * Record a tool invocation with its response time.
     * 
     * @param toolName the name of the tool that was invoked
     * @param responseTime the time it took to execute the tool
     */
    void recordToolInvocation(String toolName, Duration responseTime);
    
    /**
     * Record resource access with response size.
     * 
     * @param resourceId the identifier of the resource accessed
     * @param responseSize the size of the response in bytes
     */
    void recordResourceAccess(String resourceId, long responseSize);
    
    /**
     * Generate a performance report for the given time range.
     * 
     * @param timeRange the time range for the report
     * @return performance report containing aggregated metrics
     */
    PerformanceReport generateReport(TimeRange timeRange);
    
    /**
     * Set alert thresholds for performance monitoring.
     * 
     * @param thresholds map of metric names to threshold values
     */
    void setAlertThresholds(Map<String, Threshold> thresholds);
}