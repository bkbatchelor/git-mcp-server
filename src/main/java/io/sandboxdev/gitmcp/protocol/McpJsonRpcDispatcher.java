package io.sandboxdev.gitmcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Central JSON-RPC dispatcher for MCP protocol messages.
 * 
 * This component handles the routing and validation of MCP JSON-RPC 2.0 messages,
 * ensuring protocol compliance and proper error handling.
 */
@Component
public class McpJsonRpcDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(McpJsonRpcDispatcher.class);
    private final ObjectMapper objectMapper;
    
    // Known MCP methods - will be expanded as we implement more functionality
    private static final Set<String> KNOWN_METHODS = Set.of(
        "initialize",
        "tools/list",
        "tools/call", 
        "resources/list",
        "resources/read",
        "notifications/message"
    );

    public McpJsonRpcDispatcher() {
        this.objectMapper = new ObjectMapper();
    }

    public McpJsonRpcDispatcher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Dispatches an MCP request and returns the appropriate response.
     * 
     * @param request The MCP request to process
     * @param context The message context (can be null for testing)
     * @return The MCP response
     */
    public McpResponse dispatch(McpRequest request, MessageContext context) {
        logger.debug("Dispatching MCP request: method={}, id={}", request.method(), request.id());
        
        try {
            // Validate the request
            if (!isValidRequest(request)) {
                return createErrorResponse(
                    McpError.invalidRequest("Invalid JSON-RPC request format"),
                    request.id()
                );
            }
            
            // Check if method is known
            if (!KNOWN_METHODS.contains(request.method())) {
                return createErrorResponse(
                    McpError.methodNotFound(request.method()),
                    request.id()
                );
            }
            
            // For now, return a placeholder success response
            // This will be expanded when we implement actual tool and resource handlers
            JsonNode result = objectMapper.createObjectNode()
                .put("status", "success")
                .put("message", "Method " + request.method() + " processed successfully");
                
            return McpResponse.success(result, request.id());
            
        } catch (Exception e) {
            logger.error("Error processing MCP request: {}", e.getMessage(), e);
            return createErrorResponse(
                McpError.internalError("Internal server error: " + e.getMessage()),
                request.id()
            );
        }
    }

    /**
     * Handles malformed JSON that couldn't be parsed into a valid request.
     * 
     * @param malformedJson The malformed JSON string
     * @return An error response with parse error
     */
    public McpResponse handleMalformedJson(String malformedJson) {
        logger.warn("Received malformed JSON: {}", malformedJson);
        
        return createErrorResponse(
            McpError.parseError("Invalid JSON format"),
            null // No ID available for malformed JSON
        );
    }

    /**
     * Handles a notification message (no response expected).
     * 
     * @param notification The MCP notification
     * @param context The message context (can be null for testing)
     */
    public void handleNotification(McpNotification notification, MessageContext context) {
        logger.debug("Handling MCP notification: method={}", notification.method());
        
        if (!notification.isValid()) {
            logger.warn("Invalid notification received: {}", notification);
            return;
        }
        
        // Process the notification based on method
        switch (notification.method()) {
            case "notifications/message" -> handleLogNotification(notification);
            default -> logger.warn("Unknown notification method: {}", notification.method());
        }
    }

    /**
     * Validates that a request conforms to MCP JSON-RPC 2.0 specification.
     */
    private boolean isValidRequest(McpRequest request) {
        if (request == null) {
            return false;
        }
        
        // Check JSON-RPC version
        if (!"2.0".equals(request.jsonrpc())) {
            return false;
        }
        
        // Check method name
        if (request.method() == null || request.method().isBlank()) {
            return false;
        }
        
        // Check request ID
        if (request.id() == null || request.id().isBlank()) {
            return false;
        }
        
        return true;
    }

    /**
     * Creates an error response with proper JSON-RPC format.
     */
    private McpResponse createErrorResponse(McpError error, String requestId) {
        return McpResponse.error(error, requestId != null ? requestId : "unknown");
    }

    /**
     * Handles log notification messages.
     */
    private void handleLogNotification(McpNotification notification) {
        try {
            JsonNode params = notification.params();
            if (params != null && params.has("level") && params.has("message")) {
                String level = params.get("level").asText();
                String message = params.get("message").asText();
                
                // Forward to appropriate log level
                switch (level.toLowerCase()) {
                    case "error" -> logger.error("MCP Client: {}", message);
                    case "warn" -> logger.warn("MCP Client: {}", message);
                    case "info" -> logger.info("MCP Client: {}", message);
                    case "debug" -> logger.debug("MCP Client: {}", message);
                    default -> logger.info("MCP Client [{}]: {}", level, message);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to process log notification: {}", e.getMessage());
        }
    }
}