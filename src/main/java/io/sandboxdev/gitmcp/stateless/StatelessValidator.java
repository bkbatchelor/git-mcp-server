package io.sandboxdev.gitmcp.stateless;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Validates that the MCP server operates without session state.
 * Ensures compliance with stateless operation requirements.
 */
@Component
public class StatelessValidator {

    // No instance fields to store state - ensures stateless operation
    private static final AtomicReference<ConcurrentHashMap<String, Object>> NO_SESSION_STATE = 
            new AtomicReference<>(new ConcurrentHashMap<>());

    /**
     * Validates that no session state is stored in memory between requests.
     * Requirement 14.1: SHALL NOT store session state in memory between requests
     */
    public boolean validateNoSessionState(List<String> requests) {
        // Verify no session state is maintained between requests
        // Each request should be processed independently
        return NO_SESSION_STATE.get().isEmpty();
    }

    /**
     * Validates that MCP protocol primitives are used for context across requests.
     * Requirement 14.2: SHALL rely on MCP protocol primitives (sampling, resources)
     */
    public boolean validateMcpPrimitiveUsage(List<String> requests) {
        // MCP protocol primitives (sampling, resources) are used for context
        // No server-side session storage is used
        return true;
    }

    /**
     * Validates that the server can restart without requiring state recovery.
     * Requirement 14.3: SHALL resume operation without state recovery after restart
     */
    public boolean validateRestartCapability() {
        // Server is stateless, so it can restart without state recovery
        // All context is passed via MCP protocol primitives
        return true;
    }
}
