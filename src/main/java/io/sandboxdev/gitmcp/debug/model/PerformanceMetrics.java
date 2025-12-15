package io.sandboxdev.gitmcp.debug.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents performance metrics for a single operation.
 */
public record PerformanceMetrics(
    String toolName,
    Duration responseTime,
    long responseSize,
    Instant timestamp,
    boolean success
) {}