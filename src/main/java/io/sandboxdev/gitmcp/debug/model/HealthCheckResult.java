package io.sandboxdev.gitmcp.debug.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the result of a health check execution.
 */
public record HealthCheckResult(
    String checkName,
    HealthStatus status,
    String message,
    Instant timestamp,
    Map<String, Object> details
) {}