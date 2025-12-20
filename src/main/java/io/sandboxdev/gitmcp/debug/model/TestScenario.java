package io.sandboxdev.gitmcp.debug.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a test scenario for client simulation.
 */
public record TestScenario(
    String name,
    String description,
    List<TestOperation> operations
) {
    
    /**
     * Represents a single operation in a test scenario.
     */
    public record TestOperation(
        String toolName,
        Map<String, Object> parameters,
        Map<String, Object> expectedResponse
    ) {}
}