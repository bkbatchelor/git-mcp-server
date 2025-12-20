package io.sandboxdev.gitmcp.debug.model;

import java.time.Instant;

/**
 * Represents a time range for filtering and reporting.
 */
public record TimeRange(
    Instant start,
    Instant end
) {
    
    /**
     * Create a time range from now back to the specified duration.
     */
    public static TimeRange lastDuration(java.time.Duration duration) {
        Instant now = Instant.now();
        return new TimeRange(now.minus(duration), now);
    }
    
    /**
     * Create a time range for the last hour.
     */
    public static TimeRange lastHour() {
        return lastDuration(java.time.Duration.ofHours(1));
    }
    
    /**
     * Create a time range for the last day.
     */
    public static TimeRange lastDay() {
        return lastDuration(java.time.Duration.ofDays(1));
    }
}