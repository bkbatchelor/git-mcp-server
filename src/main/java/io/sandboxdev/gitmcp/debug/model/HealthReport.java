package io.sandboxdev.gitmcp.debug.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents a comprehensive health report.
 */
public record HealthReport(
    Instant generatedAt,
    HealthStatus overallStatus,
    List<HealthCheckResult> componentResults,
    String summary
) {}