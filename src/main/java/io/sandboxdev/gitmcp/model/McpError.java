package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents an MCP JSON-RPC 2.0 error object.
 * 
 * This record encapsulates error information according to the JSON-RPC specification:
 * - code: A number indicating the error type
 * - message: A string providing a short description of the error
 * - data: Additional information about the error (optional)
 */
public record McpError(
    @JsonProperty("code") int code,
    @JsonProperty("message") String message,
    @JsonProperty("data") JsonNode data
) {
    
    /**
     * Creates an error without additional data.
     */
    public McpError(int code, String message) {
        this(code, message, null);
    }
    
    // Standard JSON-RPC error codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    
    // MCP-specific error codes (application-defined range)
    public static final int REPOSITORY_NOT_FOUND = -32000;
    public static final int INVALID_REPOSITORY_STATE = -32001;
    public static final int PERMISSION_DENIED = -32002;
    public static final int PATH_TRAVERSAL_DETECTED = -32003;
    public static final int OPERATION_TIMEOUT = -32004;
    
    /**
     * Creates a parse error.
     */
    public static McpError parseError(String message) {
        return new McpError(PARSE_ERROR, message);
    }
    
    /**
     * Creates an invalid request error.
     */
    public static McpError invalidRequest(String message) {
        return new McpError(INVALID_REQUEST, message);
    }
    
    /**
     * Creates a method not found error.
     */
    public static McpError methodNotFound(String method) {
        return new McpError(METHOD_NOT_FOUND, "Method not found: " + method);
    }
    
    /**
     * Creates an invalid parameters error.
     */
    public static McpError invalidParams(String message) {
        return new McpError(INVALID_PARAMS, message);
    }
    
    /**
     * Creates an internal error.
     */
    public static McpError internalError(String message) {
        return new McpError(INTERNAL_ERROR, message);
    }
    
    /**
     * Creates a repository not found error.
     */
    public static McpError repositoryNotFound(String path) {
        return new McpError(REPOSITORY_NOT_FOUND, "Repository not found: " + path);
    }
    
    /**
     * Creates an invalid repository state error.
     */
    public static McpError invalidRepositoryState(String message) {
        return new McpError(INVALID_REPOSITORY_STATE, message);
    }
    
    /**
     * Creates a permission denied error.
     */
    public static McpError permissionDenied(String message) {
        return new McpError(PERMISSION_DENIED, message);
    }
    
    /**
     * Creates a path traversal detected error.
     */
    public static McpError pathTraversalDetected(String path) {
        return new McpError(PATH_TRAVERSAL_DETECTED, "Path traversal detected: " + path);
    }
    
    /**
     * Creates an operation timeout error.
     */
    public static McpError operationTimeout(String operation) {
        return new McpError(OPERATION_TIMEOUT, "Operation timeout: " + operation);
    }
}