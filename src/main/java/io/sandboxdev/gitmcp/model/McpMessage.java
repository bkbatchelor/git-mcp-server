package io.sandboxdev.gitmcp.model;

/**
 * Base sealed interface for all MCP protocol messages.
 * 
 * This interface ensures type safety by restricting implementations to the
 * three core MCP message types: requests, responses, and notifications.
 */
public sealed interface McpMessage 
    permits McpRequest, McpResponse, McpNotification {
    
    /**
     * The JSON-RPC version, always "2.0" for MCP protocol compliance.
     */
    String jsonrpc();
}