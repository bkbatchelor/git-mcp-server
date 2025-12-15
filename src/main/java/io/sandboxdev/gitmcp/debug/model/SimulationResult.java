package io.sandboxdev.gitmcp.debug.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the result of a client simulation.
 */
public record SimulationResult(
    String simulationId,
    Instant timestamp,
    Duration executionTime,
    boolean success,
    Map<String, Object> response,
    Optional<String> errorMessage,
    Map<String, Object> metrics
) {}