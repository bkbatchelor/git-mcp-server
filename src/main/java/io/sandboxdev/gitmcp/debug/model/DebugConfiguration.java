package io.sandboxdev.gitmcp.debug.model;

import java.time.Duration;
import java.util.Map;

/**
 * Represents the debug system configuration.
 */
public record DebugConfiguration(
    DebugLevel globalLevel,
    Map<String, DebugLevel> componentLevels,
    boolean protocolTracingEnabled,
    boolean performanceMonitoringEnabled,
    Duration traceRetention,
    int maxTraceBufferSize
) {}