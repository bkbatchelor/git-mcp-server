package io.sandboxdev.gitmcp.mcp;

import io.sandboxdev.gitmcp.exception.GitMcpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles exceptions and translates them to MCP error format.
 * Ensures all errors are logged and returned in a consistent format.
 */
@Component
public class McpErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpErrorHandler.class);

    /**
     * Translates a GitMcpException to MCP error format.
     * 
     * @param exception The exception to translate
     * @param operation The operation that failed
     * @return Map containing MCP-formatted error response
     */
    public Map<String, Object> handleGitMcpException(GitMcpException exception, String operation) {
        logger.error("Git MCP operation failed: {} - Error: {} - Message: {}", 
            operation, exception.getErrorCode(), exception.getMessage(), exception);
        
        Map<String, Object> errorData = new HashMap<>(exception.getDetails());
        errorData.put("operation", operation);
        
        return createErrorResponse(
            exception.getErrorCode().toString(),
            exception.getMessage(),
            errorData
        );
    }

    /**
     * Translates a generic exception to MCP error format.
     * 
     * @param exception The exception to translate
     * @param operation The operation that failed
     * @return Map containing MCP-formatted error response
     */
    public Map<String, Object> handleGenericException(Exception exception, String operation) {
        logger.error("Unexpected error during operation: {} - Message: {}", 
            operation, exception.getMessage(), exception);
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("operation", operation);
        errorData.put("exceptionType", exception.getClass().getSimpleName());
        
        return createErrorResponse(
            "OPERATION_FAILED",
            "An unexpected error occurred: " + exception.getMessage(),
            errorData
        );
    }

    /**
     * Creates an MCP-formatted error response.
     * 
     * @param code Error code
     * @param message Error message
     * @param data Additional error data
     * @return Map containing MCP error format
     */
    private Map<String, Object> createErrorResponse(String code, String message, Map<String, Object> data) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        
        if (data != null && !data.isEmpty()) {
            error.put("data", data);
        }
        
        return Map.of("error", error);
    }

    /**
     * Formats an error response for MCP protocol.
     * This method can be used by tool providers to wrap exceptions.
     * 
     * @param throwable The throwable to format
     * @param operation The operation that failed
     * @return Map containing MCP-formatted error response
     */
    public Map<String, Object> formatError(Throwable throwable, String operation) {
        if (throwable instanceof GitMcpException gitMcpException) {
            return handleGitMcpException(gitMcpException, operation);
        } else if (throwable instanceof Exception exception) {
            return handleGenericException(exception, operation);
        } else {
            logger.error("Unexpected throwable during operation: {}", operation, throwable);
            return createErrorResponse(
                "OPERATION_FAILED",
                "An unexpected error occurred",
                Map.of("operation", operation)
            );
        }
    }
}
