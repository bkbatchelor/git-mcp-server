package io.sandboxdev.gitmcp.protocol;

import java.util.Map;
import java.util.Optional;

/**
 * Context information for processing MCP messages.
 * 
 * This record carries contextual information that may be needed
 * during message processing, such as authentication details,
 * request metadata, or session information.
 */
public record MessageContext(
    Optional<String> sessionId,
    Optional<String> userId,
    Map<String, String> headers,
    long timestamp
) {
    
    /**
     * Creates a new message context with current timestamp.
     */
    public static MessageContext create() {
        return new MessageContext(
            Optional.empty(),
            Optional.empty(),
            Map.of(),
            System.currentTimeMillis()
        );
    }
    
    /**
     * Creates a message context with session ID.
     */
    public static MessageContext withSession(String sessionId) {
        return new MessageContext(
            Optional.of(sessionId),
            Optional.empty(),
            Map.of(),
            System.currentTimeMillis()
        );
    }
    
    /**
     * Creates a message context with headers.
     */
    public static MessageContext withHeaders(Map<String, String> headers) {
        return new MessageContext(
            Optional.empty(),
            Optional.empty(),
            Map.copyOf(headers),
            System.currentTimeMillis()
        );
    }
}