package io.sandboxdev.gitmcp.debug.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents an error analysis report.
 */
public record ErrorAnalysisReport(
    TimeRange timeRange,
    Instant generatedAt,
    Map<String, Long> errorCountsByType,
    List<ErrorPattern> commonPatterns,
    Map<String, Double> errorRatesByTool
) {
    
    /**
     * Represents a common error pattern.
     */
    public record ErrorPattern(
        String pattern,
        long occurrences,
        String description
    ) {}
}