package io.sandboxdev.gitmcp.debug.model;

import java.time.Duration;
import java.util.List;

/**
 * Represents an alert rule configuration.
 */
public record AlertRule(
    String name,
    AlertCondition condition,
    AlertSeverity severity,
    List<String> notificationChannels,
    Duration cooldownPeriod
) {}