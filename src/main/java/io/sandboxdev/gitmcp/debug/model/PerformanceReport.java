package io.sandboxdev.gitmcp.debug.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Represents an aggregated performance report.
 */
public record PerformanceReport(
    TimeRange timeRange,
    Instant generatedAt,
    Map<String, Duration> averageResponseTimes,
    Map<String, Duration> percentile95ResponseTimes,
    Map<String, Duration> maxResponseTimes,
    Map<String, Long> totalInvocations,
    Map<String, Double> successRates
) {}