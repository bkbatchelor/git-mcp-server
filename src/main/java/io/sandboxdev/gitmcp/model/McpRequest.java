package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents an MCP JSON-RPC 2.0 request message.
 * 
 * This record encapsulates all the required fields for a valid MCP request:
 * - jsonrpc: Protocol version (always "2.0")
 * - method: The name of the method to be invoked
 * - params: Parameters for the method (can be null)
 * - id: Unique identifier for the request
 */
public record McpRequest(
    @JsonProperty("jsonrpc") String jsonrpc,
    @JsonProperty("method") String method,
    @JsonProperty("params") JsonNode params,
    @JsonProperty("id") String id
) implements McpMessage {
    
    /**
     * Creates a new MCP request with the standard JSON-RPC version.
     */
    public McpRequest(String method, JsonNode params, String id) {
        this("2.0", method, params, id);
    }
    
    /**
     * Validates that this request conforms to MCP specification.
     */
    @JsonIgnore
    public boolean isValid() {
        return "2.0".equals(jsonrpc) && 
               method != null && !method.isBlank() && 
               id != null && !id.isBlank();
    }
}