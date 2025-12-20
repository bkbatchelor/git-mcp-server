package io.sandboxdev.gitmcp.debug.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents the history of triggered alerts.
 */
public record AlertHistory(
    TimeRange timeRange,
    List<TriggeredAlert> alerts
) {
    
    /**
     * Represents a single triggered alert.
     */
    public record TriggeredAlert(
        String alertName,
        AlertType type,
        AlertSeverity severity,
        String message,
        Instant timestamp,
        Map<String, Object> context
    ) {}
}