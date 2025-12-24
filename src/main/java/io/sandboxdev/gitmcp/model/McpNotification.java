package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents an MCP JSON-RPC 2.0 notification message.
 * 
 * Notifications are one-way messages that do not expect a response.
 * They are used for events like logging or status updates.
 * 
 * This record encapsulates:
 * - jsonrpc: Protocol version (always "2.0")
 * - method: The name of the notification method
 * - params: Parameters for the notification (can be null)
 * 
 * Note: Notifications do not have an 'id' field.
 */
public record McpNotification(
    @JsonProperty("jsonrpc") String jsonrpc,
    @JsonProperty("method") String method,
    @JsonProperty("params") JsonNode params
) implements McpMessage {
    
    /**
     * Creates a new MCP notification with the standard JSON-RPC version.
     */
    public McpNotification(String method, JsonNode params) {
        this("2.0", method, params);
    }
    
    /**
     * Validates that this notification conforms to MCP specification.
     */
    @JsonIgnore
    public boolean isValid() {
        return "2.0".equals(jsonrpc) && 
               method != null && !method.isBlank();
    }
}