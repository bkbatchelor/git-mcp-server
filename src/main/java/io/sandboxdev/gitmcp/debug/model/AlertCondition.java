package io.sandboxdev.gitmcp.debug.model;

/**
 * Represents a condition that triggers an alert.
 */
public record AlertCondition(
    String metricName,
    Threshold threshold,
    String description
) {}