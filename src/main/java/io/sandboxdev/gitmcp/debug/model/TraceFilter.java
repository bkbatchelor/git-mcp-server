package io.sandboxdev.gitmcp.debug.model;

import java.time.Instant;
import java.util.Optional;

/**
 * Filter criteria for selecting protocol traces.
 */
public record TraceFilter(
    Optional<String> messageType,
    Optional<String> clientId,
    Optional<Instant> startTime,
    Optional<Instant> endTime,
    Optional<Boolean> hasError
) {
    
    /**
     * Create a filter that matches all traces.
     */
    public static TraceFilter all() {
        return new TraceFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
    
    /**
     * Create a filter for a specific message type.
     */
    public static TraceFilter byMessageType(String messageType) {
        return new TraceFilter(
            Optional.of(messageType),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
    
    /**
     * Create a filter for a specific client ID.
     */
    public static TraceFilter byClientId(String clientId) {
        return new TraceFilter(
            Optional.empty(),
            Optional.of(clientId),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
}