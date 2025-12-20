package io.sandboxdev.gitmcp.debug.model;

/**
 * Filter criteria for selecting protocol traces.
 */
public record TraceFilter(
    String messageType,
    String clientId,
    TimeRange timeRange,
    boolean errorsOnly
) {
    
    /**
     * Create a builder for constructing trace filters.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a filter that matches all traces.
     */
    public static TraceFilter all() {
        return new TraceFilter(null, null, null, false);
    }
    
    /**
     * Create a filter for a specific message type.
     */
    public static TraceFilter byMessageType(String messageType) {
        return new TraceFilter(messageType, null, null, false);
    }
    
    /**
     * Create a filter for a specific client ID.
     */
    public static TraceFilter byClientId(String clientId) {
        return new TraceFilter(null, clientId, null, false);
    }
    
    /**
     * Create a filter for errors only.
     */
    public static TraceFilter forErrorsOnly() {
        return new TraceFilter(null, null, null, true);
    }
    
    /**
     * Builder class for constructing TraceFilter instances.
     */
    public static class Builder {
        private String messageType;
        private String clientId;
        private TimeRange timeRange;
        private boolean errorsOnly;
        
        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }
        
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }
        
        public Builder timeRange(TimeRange timeRange) {
            this.timeRange = timeRange;
            return this;
        }
        
        public Builder errorsOnly(boolean errorsOnly) {
            this.errorsOnly = errorsOnly;
            return this;
        }
        
        public TraceFilter build() {
            return new TraceFilter(messageType, clientId, timeRange, errorsOnly);
        }
    }
}