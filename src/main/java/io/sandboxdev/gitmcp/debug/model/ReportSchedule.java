package io.sandboxdev.gitmcp.debug.model;

import java.time.Duration;
import java.util.List;

/**
 * Represents a schedule for automatic report generation.
 */
public record ReportSchedule(
    Duration interval,
    List<ReportType> reportTypes,
    List<String> recipients
) {
    
    /**
     * Enumeration of report types.
     */
    public enum ReportType {
        PERFORMANCE,
        ERROR_ANALYSIS,
        HEALTH
    }
}