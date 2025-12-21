package io.sandboxdev.gitmcp.debug.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a captured MCP protocol trace.
 * Contains all information about a protocol message exchange.
 */
public record ProtocolTrace(
    String traceId,
    Instant timestamp,
    String clientId,
    String messageType,
    Map<String, Object> parameters,
    Map<String, Object> response,
    Duration processingTime,
    Optional<String> errorMessage
) {}