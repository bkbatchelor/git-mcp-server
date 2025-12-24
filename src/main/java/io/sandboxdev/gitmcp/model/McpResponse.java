package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents an MCP JSON-RPC 2.0 response message.
 * 
 * This record encapsulates all the required fields for a valid MCP response:
 * - jsonrpc: Protocol version (always "2.0")
 * - result: The result of the method invocation (null if error occurred)
 * - error: Error information (null if successful)
 * - id: Identifier matching the original request
 * 
 * Note: Either result or error must be present, but not both.
 */
public record McpResponse(
    @JsonProperty("jsonrpc") String jsonrpc,
    @JsonProperty("result") JsonNode result,
    @JsonProperty("error") McpError error,
    @JsonProperty("id") String id
) implements McpMessage {
    
    /**
     * Creates a successful response with the standard JSON-RPC version.
     */
    public static McpResponse success(JsonNode result, String id) {
        return new McpResponse("2.0", result, null, id);
    }
    
    /**
     * Creates an error response with the standard JSON-RPC version.
     */
    public static McpResponse error(McpError error, String id) {
        return new McpResponse("2.0", null, error, id);
    }
    
    /**
     * Validates that this response conforms to MCP specification.
     */
    @JsonIgnore
    public boolean isValid() {
        return "2.0".equals(jsonrpc) && 
               id != null && !id.isBlank() &&
               (result != null ^ error != null); // XOR: exactly one must be non-null
    }
    
    /**
     * Returns true if this response represents a successful operation.
     */
    @JsonIgnore
    public boolean isSuccess() {
        return error == null && result != null;
    }
    
    /**
     * Returns true if this response represents an error.
     */
    @JsonIgnore
    public boolean isError() {
        return error != null && result == null;
    }
}