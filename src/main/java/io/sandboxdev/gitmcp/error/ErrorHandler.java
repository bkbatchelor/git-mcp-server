package io.sandboxdev.gitmcp.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.McpError;
import io.sandboxdev.gitmcp.model.ToolResult;
import org.springframework.stereotype.Component;

/**
 * Handles error translation and graceful degradation for Git MCP operations.
 * Implements Requirements 10.1, 10.2, 10.3, 10.4, 10.5
 */
@Component
public class ErrorHandler {

    private final ObjectMapper objectMapper;

    public ErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ToolResult handleGitOperationFailure(String type, String details) {
        String errorMessage = switch (type) {
            case "REPOSITORY_NOT_FOUND" -> "Repository not found at path: " + details + ". Please verify the path exists and is accessible.";
            case "PERMISSION_DENIED" -> "Access denied to repository: " + details + ". Please check file permissions.";
            case "INVALID_REPOSITORY_STATE" -> "Repository is in an invalid state: " + details + ". Please resolve conflicts or complete pending operations.";
            case "MERGE_CONFLICT" -> "Merge conflict detected: " + details + ". Please resolve conflicts and try again.";
            case "NO_STAGED_CHANGES" -> "No staged changes found. Please stage files before committing.";
            default -> "Git operation failed: " + details;
        };
        
        return ToolResult.error(errorMessage);
    }

    public McpError translateExceptionToMcpError(Exception exception) {
        String sanitizedMessage = sanitizeExceptionMessage(exception.getMessage());
        JsonNode errorData = createErrorData(exception.getClass().getSimpleName());
        
        return new McpError(McpError.INTERNAL_ERROR, sanitizedMessage, errorData);
    }

    public ToolResult handleInvalidRepositoryState(String state, String description) {
        String suggestion = switch (state) {
            case "DETACHED_HEAD" -> "Try to check out a branch or create a new one";
            case "MERGE_IN_PROGRESS" -> "Try to complete the merge or abort it with git merge --abort";
            case "REBASE_IN_PROGRESS" -> "Try to complete the rebase or abort it with git rebase --abort";
            case "CORRUPTED_INDEX" -> "Try to reset the index with git reset --hard";
            default -> "Please check repository status and resolve any issues";
        };
        
        String errorMessage = description + ". Suggestion: " + suggestion;
        return ToolResult.error(errorMessage);
    }

    public McpError handleInternalError(String type, String message) {
        return new McpError(McpError.INTERNAL_ERROR, message);
    }

    public ToolResult handleToolTimeout(String operation, long timeoutMs) {
        String errorMessage = String.format("Operation '%s' exceeded time limit of %d ms. The operation may be too complex or the repository too large.", 
                                           operation, timeoutMs);
        return ToolResult.error(errorMessage);
    }

    private String sanitizeExceptionMessage(String message) {
        if (message == null) {
            return "An internal error occurred";
        }
        
        // Remove stack trace elements and file references
        return message.replaceAll("at .*\\.java:\\d+", "")
                     .replaceAll("\\s+", " ")
                     .trim();
    }

    private JsonNode createErrorData(String exceptionType) {
        try {
            return objectMapper.createObjectNode()
                    .put("exceptionType", exceptionType)
                    .put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            return null;
        }
    }
}
