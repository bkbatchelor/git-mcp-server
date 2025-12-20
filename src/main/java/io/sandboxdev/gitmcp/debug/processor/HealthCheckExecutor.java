package io.sandboxdev.gitmcp.debug.processor;

import io.sandboxdev.gitmcp.debug.model.HealthCheck;
import io.sandboxdev.gitmcp.debug.model.HealthCheckResult;

import java.time.Duration;
import java.util.List;

/**
 * Interface for executing health checks on MCP server components.
 * Handles health check execution, scheduling, and result tracking.
 */
public interface HealthCheckExecutor {
    
    /**
     * Run a comprehensive health check of all system components.
     * 
     * @return the result of the health check
     */
    HealthCheckResult runHealthCheck();
    
    /**
     * Schedule health checks to run at the specified interval.
     * 
     * @param interval the interval between health check executions
     */
    void scheduleHealthChecks(Duration interval);
    
    /**
     * Get the history of health check results.
     * 
     * @return list of historical health check results
     */
    List<HealthCheckResult> getHealthHistory();
    
    /**
     * Add a custom health check to the system.
     * 
     * @param check the custom health check to add
     */
    void addCustomHealthCheck(HealthCheck check);
}