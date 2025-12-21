package io.sandboxdev.gitmcp.debug.model;

/**
 * Interface for custom health checks.
 */
public interface HealthCheck {
    
    /**
     * Execute the health check.
     * 
     * @return the result of the health check
     */
    HealthCheckResult execute();
    
    /**
     * Get the name of this health check.
     * 
     * @return the health check name
     */
    String getName();
}