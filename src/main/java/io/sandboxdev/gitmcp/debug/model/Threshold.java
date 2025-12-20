package io.sandboxdev.gitmcp.debug.model;

/**
 * Represents a threshold value for alerting.
 */
public record Threshold(
    double value,
    ThresholdType type
) {
    
    public enum ThresholdType {
        GREATER_THAN,
        LESS_THAN,
        EQUALS
    }
    
    /**
     * Create a "greater than" threshold.
     */
    public static Threshold greaterThan(double value) {
        return new Threshold(value, ThresholdType.GREATER_THAN);
    }
    
    /**
     * Create a "less than" threshold.
     */
    public static Threshold lessThan(double value) {
        return new Threshold(value, ThresholdType.LESS_THAN);
    }
}